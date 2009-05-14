/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pivot.wtk.skin;

import pivot.collections.Sequence;
import pivot.util.Vote;
import pivot.wtk.CardPane;
import pivot.wtk.CardPaneListener;
import pivot.wtk.Component;
import pivot.wtk.Container;
import pivot.wtk.Dimensions;
import pivot.wtk.Orientation;
import pivot.wtk.effects.Transition;
import pivot.wtk.effects.TransitionListener;
import pivot.wtk.effects.easing.Easing;
import pivot.wtk.effects.easing.Quartic;

/**
 * Card pane skin.
 *
 * @author gbrown
 */
public class CardPaneSkin extends ContainerSkin implements CardPaneListener {
    /**
     * Abstract base class for selection change transitions.
     *
     * @author gbrown
     */
    public abstract class SelectionChangeTransition extends Transition {
        private int from;
        private int to;

        public SelectionChangeTransition(int from, int to) {
            super(SELECTION_CHANGE_DURATION, SELECTION_CHANGE_RATE, false);

            this.from = from;
            this.to = to;
        }

        public int getFrom() {
            return from;
        }

        public Component getFromCard() {
            CardPane cardPane = (CardPane)getComponent();
            return (from == -1) ? null : cardPane.get(from);
        }

        public int getTo() {
            return to;
        }

        public Component getToCard() {
            CardPane cardPane = (CardPane)getComponent();
            return (to == -1) ? null : cardPane.get(to);
        }
    }

    /**
     * Class that performs slide selection change transitions.
     *
     * @author gbrown
     */
    public class SlideTransition extends SelectionChangeTransition {
        private Easing slideEasing = new Quartic();

        public SlideTransition(int from, int to) {
            super(from, to);
        }

        @Override
        public void start(TransitionListener transitionListener) {
            Component toCard = getToCard();
            toCard.setVisible(true);

            super.start(transitionListener);
        }

        @Override
        public void stop() {
            Component fromCard = getFromCard();
            fromCard.setVisible(false);

            super.stop();
        }

        @Override
        protected void update() {
            CardPane cardPane = (CardPane)getComponent();

            int width = getWidth();
            int height = getHeight();

            float percentComplete = slideEasing.easeOut(getElapsedTime(), 0, 1, getDuration());

            int from = getFrom();
            int to = getTo();

            int direction = Integer.signum(from - to);

            int dx = (int)((float)width * percentComplete) * direction;
            int dy = (int)((float)height * percentComplete) * direction;

            Component fromCard = cardPane.get(from);
            Component toCard = cardPane.get(to);

            if (orientation == Orientation.HORIZONTAL) {
                fromCard.setLocation(dx, 0);
                toCard.setLocation(-width * direction + dx, 0);
            } else {
                fromCard.setLocation(0, dy);
                toCard.setLocation(0, -height * direction + dy);
            }
        }
    }

    private Orientation orientation = null;

    private SelectionChangeTransition selectionChangeTransition = null;

    public static final int SELECTION_CHANGE_DURATION = 250;
    public static final int SELECTION_CHANGE_RATE = 30;

    public void install(Component component) {
        super.install(component);

        CardPane cardPane = (CardPane)component;
        cardPane.getCardPaneListeners().add(this);
    }

    public void uninstall() {
        CardPane cardPane = (CardPane)getComponent();
        cardPane.getCardPaneListeners().remove(this);

        super.uninstall();
    }

    public int getPreferredWidth(int height) {
        int preferredWidth = 0;

        if (height == -1) {
            Dimensions preferredSize = getPreferredSize();
            preferredWidth = preferredSize.width;
        } else {
            CardPane cardPane = (CardPane)getComponent();
            for (Component card : cardPane) {
                preferredWidth = Math.max(preferredWidth, card.getPreferredWidth(height));
            }
        }

        return preferredWidth;
    }

    public int getPreferredHeight(int width) {
        int preferredHeight = 0;

        if (width == -1) {
            Dimensions preferredSize = getPreferredSize();
            preferredHeight = preferredSize.height;
        } else {
            CardPane cardPane = (CardPane)getComponent();
            for (Component card : cardPane) {
                preferredHeight = Math.max(preferredHeight, card.getPreferredHeight(width));
            }
        }

        return preferredHeight;
    }

    public Dimensions getPreferredSize() {
        Dimensions preferredSize;

        int preferredWidth = 0;
        int preferredHeight = 0;

        CardPane cardPane = (CardPane)getComponent();

        for (Component card : cardPane) {
            Dimensions cardSize = card.getPreferredSize();

            preferredWidth = Math.max(cardSize.width, preferredWidth);
            preferredHeight = Math.max(cardSize.height, preferredHeight);
        }

        preferredSize = new Dimensions(preferredWidth, preferredHeight);

        return preferredSize;
    }

    public void layout() {
        CardPane cardPane = (CardPane)getComponent();
        int width = getWidth();
        int height = getHeight();

        for (Component card : cardPane) {
            // If the card is visible, set its size and location
            if (card.isVisible()) {
                card.setLocation(0, 0);
                card.setSize(width, height);
            }
        }
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
    }

    public void setOrientation(String orientation) {
        if (orientation == null) {
            throw new IllegalArgumentException();
        }

        setOrientation(Orientation.decode(orientation));
    }

    @Override
    public void componentInserted(Container container, int index) {
        if (selectionChangeTransition != null) {
            selectionChangeTransition.stop();
            selectionChangeTransition = null;
        }

        super.componentInserted(container, index);

        Component card = container.get(index);
        card.setVisible(false);

        invalidateComponent();
    }

    @Override
    public void componentsRemoved(Container container, int index, Sequence<Component> removed) {
        if (selectionChangeTransition != null) {
            selectionChangeTransition.stop();
            selectionChangeTransition = null;
        }

        super.componentsRemoved(container, index, removed);

        for (int i = 0, n = removed.getLength(); i < n; i++){
            Component card = removed.get(i);
            card.setVisible(true);
        }

        invalidateComponent();
    }

    public Vote previewSelectedIndexChange(CardPane cardPane, int selectedIndex) {
        Vote vote;

        if (cardPane.isShowing()
            && selectionChangeTransition == null
            && orientation != null) {
            int previousSelectedIndex = cardPane.getSelectedIndex();

            // TODO We may want to create a FadeOut transition when going from
            // selected to deselected or a FadeIn transition for the opposite
            if (previousSelectedIndex != -1
                && selectedIndex != -1) {
                selectionChangeTransition = new SlideTransition(previousSelectedIndex, selectedIndex);
            }

            // TODO Instantiate other transition types as appropriate (based
            // on style property)

            if (selectionChangeTransition != null) {
                selectionChangeTransition.start(new TransitionListener() {
                    public void transitionCompleted(Transition transition) {
                        CardPane cardPane = (CardPane)getComponent();

                        SelectionChangeTransition selectionChangeTransition =
                            (SelectionChangeTransition)transition;
                        cardPane.setSelectedIndex(selectionChangeTransition.getTo());
                        CardPaneSkin.this.selectionChangeTransition = null;

                        invalidateComponent();
                    }
                });
            }
        }

        if (selectionChangeTransition == null
            || !selectionChangeTransition.isRunning()) {
            vote = Vote.APPROVE;
        } else {
            vote = Vote.DEFER;
        }

        return vote;
    }

    public void selectedIndexChangeVetoed(CardPane cardPane, Vote reason) {
        if (reason == Vote.DENY
            && selectionChangeTransition != null) {
            selectionChangeTransition.stop();
            selectionChangeTransition = null;
            invalidateComponent();
        }
    }

    public void selectedIndexChanged(CardPane cardPane, int previousSelectedIndex) {
        int selectedIndex = cardPane.getSelectedIndex();
        if (selectedIndex != -1) {
            Component selectedCard = cardPane.get(selectedIndex);
            selectedCard.setVisible(true);
        }

        if (previousSelectedIndex != -1) {
            Component previousSelectedCard = cardPane.get(previousSelectedIndex);
            previousSelectedCard.setVisible(false);
        }

        if (selectedIndex == -1
            || previousSelectedIndex == -1) {
            invalidateComponent();
        }
    }
}
