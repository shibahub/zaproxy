/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2011 The ZAP Development Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zaproxy.zap.extension.httppanel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import org.apache.commons.configuration.FileConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.extension.AbstractPanel;
import org.parosproxy.paros.extension.edit.ExtensionEdit;
import org.parosproxy.paros.network.HttpMessage;
import org.parosproxy.paros.view.FindDialog;
import org.parosproxy.paros.view.View;
import org.zaproxy.zap.extension.httppanel.component.HttpPanelComponentInterface;
import org.zaproxy.zap.extension.httppanel.view.HttpPanelDefaultViewSelector;
import org.zaproxy.zap.extension.httppanel.view.HttpPanelView;
import org.zaproxy.zap.extension.search.SearchMatch;
import org.zaproxy.zap.extension.search.SearchableHttpPanelComponent;

@SuppressWarnings("serial")
public abstract class HttpPanel extends AbstractPanel {

    public enum OptionsLocation {
        BEGIN,
        AFTER_COMPONENTS,
        END
    }

    private static final long serialVersionUID = 5221591643257366570L;

    private static final Logger logger = LogManager.getLogger(HttpPanel.class);

    private static final String NO_SUITABLE_COMPONENT_FOUND_LABEL =
            Constant.messages.getString("http.panel.noSuitableComponentFound");

    private static final String HTTP_PANEL_KEY = "httppanel.";
    private static final String COMPONENTS_KEY = "components.";
    private static final String DEFAULT_COMPONENT_KEY = "defaultcomponent";

    private static Comparator<HttpPanelComponentInterface> componentsComparator;
    private static Action findAction;

    private JPanel panelHeader;
    private JPanel panelContent;

    private boolean isEditable = false;
    private boolean isEnableViewSelect = false;
    protected Message message;

    private String baseConfigurationKey;
    private String componentsConfigurationKey;

    private List<DisplayedMessageChangedListener> displayedMessageChangedListeners =
            new ArrayList<>();
    private SwitchComponentItemListener switchComponentItemListener;
    private Hashtable<String, HttpPanelComponentInterface> components = new Hashtable<>();
    private List<HttpPanelComponentInterface> enabledComponents = new ArrayList<>();
    private HttpPanelComponentInterface currentComponent;

    private JPanel noComponentsPanel;

    private String savedLastSelectedComponentName;

    private JPanel allOptions;
    private JPanel componentOptions;
    private JPanel moreOptionsComponent;
    private JToolBar toolBarComponents;
    private JToolBar toolBarMoreOptions;
    private JPanel endAllOptions;

    // POOH EDIT
    private String keepIt = "";

    public HttpPanel(boolean isEditable, String configurationKey) {
        super();

        this.isEditable = isEditable;
        this.message = null;

        setConfigurationKey(configurationKey);
        try {
            System.out.println("configurationKey: "+ configurationKey);
            if(configurationKey.indexOf("main") != -1) {
                System.out.println("configurationKey: "+ configurationKey);
                this.keepIt = "main";
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        initialize();
        initUi();
        initSpecial();
    }

    protected abstract void initComponents();
    protected  void initComponents2(String keepIt){};

    protected abstract void initSpecial();

    private void setConfigurationKey(String key) {
        baseConfigurationKey = key + HTTP_PANEL_KEY;
        componentsConfigurationKey = baseConfigurationKey + COMPONENTS_KEY;
    }

    private void initialize() {
        this.setLayout(new BorderLayout());

        this.add(getPanelHeader(), BorderLayout.NORTH);
        this.add(getPanelContent(), BorderLayout.CENTER);
    }

    private void initUi() {

        allOptions = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));

        componentOptions = new JPanel(new BorderLayout(0, 0));
        moreOptionsComponent = new JPanel(new BorderLayout(0, 0));

        toolBarComponents = new JToolBar();
        toolBarComponents.setFloatable(false);
        toolBarComponents.setBorder(BorderFactory.createEmptyBorder());
        toolBarComponents.setRollover(true);

        toolBarMoreOptions = new JToolBar();
        toolBarMoreOptions.setFloatable(false);
        toolBarMoreOptions.setBorder(BorderFactory.createEmptyBorder());
        toolBarMoreOptions.setRollover(true);
        toolBarMoreOptions.getActionMap().put("findAction", getFindAction());
        toolBarMoreOptions
                .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(ExtensionEdit.getFindDefaultKeyStroke(), "findAction");

        endAllOptions = new JPanel();

        JPanel panel1 = new JPanel(new BorderLayout(0, 0));

        JPanel panelFlow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));

        panelFlow.add(allOptions);
        panelFlow.add(componentOptions);
        panelFlow.add(toolBarComponents);
        panelFlow.add(moreOptionsComponent);
        panelFlow.add(toolBarMoreOptions);
        panel1.add(panelFlow, BorderLayout.WEST);

        panelFlow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        panelFlow.add(endAllOptions);

        panel1.add(panelFlow, BorderLayout.EAST);

        panelHeader.add(panel1, BorderLayout.NORTH);

        // getPanelContent().add(new EmptyComponent(), "");

        initComponents();
        initComponents2(this.keepIt);

        setMessage(null);
    }

    private JPanel getPanelContent() {
        if (panelContent == null) {
            panelContent = new JPanel(new CardLayout());
        }
        return panelContent;
    }

    private JPanel getPanelHeader() {
        if (panelHeader == null) {
            panelHeader = new JPanel(new BorderLayout());
        }
        return panelHeader;
    }

    private static Action getFindAction() {
        if (findAction == null) {
            findAction =
                    new AbstractAction("findAction") {
                        private static final long serialVersionUID = 8365172573382425660L;

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            Component focusOwner =
                                    KeyboardFocusManager.getCurrentKeyboardFocusManager()
                                            .getFocusOwner();
                            if (focusOwner instanceof JTextComponent) {
                                FindDialog findDialog =
                                        FindDialog.getDialog(
                                                SwingUtilities.getWindowAncestor(focusOwner),
                                                false);
                                findDialog.setLastInvoker((JTextComponent) focusOwner);
                            }
                        }
                    };
        }
        return findAction;
    }

    public void addDisplayedMessageChangedListener(
            DisplayedMessageChangedListener messageChangedListener) {
        displayedMessageChangedListeners.add(messageChangedListener);
    }

    public void removeDisplayedMessageChangedListener(
            DisplayedMessageChangedListener messageChangedListener) {
        displayedMessageChangedListeners.remove(messageChangedListener);
    }

    public void setMessage(Message msg) {
        Message oldMessage = this.message;
        this.message = msg;
        
        for (HttpPanelComponentInterface component : components.values()) {
            if (!component.isEnabled(message)) {
                if (enabledComponents.contains(component)) {
                    disableComponent(component);
                }
            } else if (!enabledComponents.contains(component)) {
                enableComponent(component);
            }
        }

        if (enabledComponents.isEmpty()) {
            currentComponent = null;
            switchEmptyComponent();
            notifyDisplayedMessageChangedListeners(oldMessage, message);
            return;
        }

        boolean switchView = true;
        if (currentComponent != null
                && enabledComponents.contains(components.get(currentComponent.getName()))) {
            switchView = false;
        }

        this.validate();

        if (switchView) {
            switchComponent(enabledComponents.get(0).getName());
        } else {
            
           
            updateContent();
        }
        notifyDisplayedMessageChangedListeners(oldMessage, message);
    }

    private void notifyDisplayedMessageChangedListeners(Message oldMessage, Message message) {
        for (DisplayedMessageChangedListener changedListener : displayedMessageChangedListeners) {
            changedListener.messageChanged(oldMessage, message);
        }
    }

    protected HttpPanelComponentInterface getCurrentComponent() {
        return currentComponent;
    }

    public Message getMessage() {
        return message;
    }

    public void setEditable(boolean editable) {
        if (isEditable != editable) {
            isEditable = editable;

            synchronized (components) {
                Iterator<HttpPanelComponentInterface> it = components.values().iterator();
                while (it.hasNext()) {
                    it.next().setEditable(editable);
                }
            }
        }
    }

    public boolean isEditable() {
        return isEditable;
    }

    public void clearView() {
        setMessage(null);

        if (currentComponent != null) {
            currentComponent.clearView();
        }
    }

    public void setEnableViewSelect(boolean enableViewSelect) {
        if (isEnableViewSelect != enableViewSelect) {
            isEnableViewSelect = enableViewSelect;

            synchronized (components) {
                Iterator<HttpPanelComponentInterface> it = components.values().iterator();
                while (it.hasNext()) {
                    HttpPanelComponentInterface component = it.next();
                    component.setEnableViewSelect(enableViewSelect);
                    component.getButton().setEnabled(enableViewSelect);
                }
            }
        }
    }

    public boolean isEnableViewSelect() {
        return isEnableViewSelect;
    }

    public void clearView(boolean enableViewSelect) {
        clearView();
        setEnableViewSelect(enableViewSelect);
    }

    public void setMessage(Message aMessage, boolean enableViewSelect) {
       
        setMessage(aMessage);
        setEnableViewSelect(enableViewSelect);
    }

    public void updateContent() {
        if (currentComponent != null) {
           
            currentComponent.setMessage(message);
        }
    }

    /**
     * Saves the data shown in the views into the current message.
     *
     * <p>Has not effect if there's no UI component or message.
     *
     * @throws InvalidMessageDataException if unable to save the data (e.g. malformed).
     */
    public void saveData() {
        if (message == null || currentComponent == null) {
            return;
        }

        currentComponent.save();
    }

    private void switchComponent(String name) {
        if (isCurrentComponent(name)) {
            currentComponent.setSelected(true);
            return;
        }

        HttpPanelComponentInterface newComponent = components.get(name);

        if (newComponent == null) {
            logger.info("No component found with name: {}", name);
            return;
        }

        if (this.currentComponent != null) {
            currentComponent.setSelected(false);
            currentComponent.clearView();

            if (currentComponent.getOptionsPanel() != null) {
                componentOptions.remove(0);
            }

            if (currentComponent.getMoreOptionsPanel() != null) {
                moreOptionsComponent.remove(0);
            }
        }

        HttpPanelComponentInterface previousComponent = currentComponent;
        this.currentComponent = newComponent;

        updateContent();

        JPanel componentOptionsPanel = currentComponent.getOptionsPanel();
        if (componentOptionsPanel != null) {
            componentOptions.add(componentOptionsPanel);
        }
        componentOptions.validate();

        JPanel componentMoreOptionsPanel = currentComponent.getMoreOptionsPanel();
        if (componentMoreOptionsPanel != null) {
            moreOptionsComponent.add(componentMoreOptionsPanel);
        }
        moreOptionsComponent.validate();

        ((CardLayout) getPanelContent().getLayout()).show(panelContent, name);

        currentComponent.setSelected(true);
        fireComponentChangedEvent(previousComponent, currentComponent);
    }

    private boolean isCurrentComponent(String name) {
        return currentComponent != null && currentComponent.getName().equals(name);
    }

    protected List<HttpPanelComponentInterface> getEnabledComponents() {
        return enabledComponents;
    }

    private void switchEmptyComponent() {
        if (this.currentComponent != null) {
            currentComponent.setSelected(false);
            currentComponent.clearView();

            if (currentComponent.getOptionsPanel() != null) {
                componentOptions.remove(0);
            }

            if (currentComponent.getMoreOptionsPanel() != null) {
                moreOptionsComponent.remove(0);
            }

            currentComponent = null;
        }

        if (noComponentsPanel == null) {
            noComponentsPanel = new JPanel(new BorderLayout(5, 5));
            noComponentsPanel.add(new JLabel(NO_SUITABLE_COMPONENT_FOUND_LABEL));
            getPanelContent().add(new JScrollPane(noComponentsPanel), "");
        }
        componentOptions.removeAll();
        componentOptions.validate();
        ((CardLayout) getPanelContent().getLayout()).show(panelContent, "");
    }

    public void addOptions(Component comp, OptionsLocation location) {

        switch (location) {
            case BEGIN:
                allOptions.add(comp);
                break;
            case AFTER_COMPONENTS:
                toolBarMoreOptions.add(comp);
                break;
            case END:
                endAllOptions.add(comp);
                break;
            default:
                break;
        }
    }

    public void addOptionsSeparator() {
        toolBarMoreOptions.addSeparator();
    }

    private void addComponent(HttpPanelComponentInterface component) {
        synchronized (components) {
            final String componentName = component.getName();
            if (components.containsKey(componentName)) {
                removeComponent(componentName);
            }

            component.setEditable(isEditable);
            component.setEnableViewSelect(isEnableViewSelect);

            components.put(componentName, component);
            panelContent.add(component.getMainPanel(), componentName);

            final JToggleButton button = component.getButton();
            button.setActionCommand(componentName);

            button.addActionListener(getSwitchComponentItemListener());
            button.setEnabled(isEnableViewSelect);

            if (component.isEnabled(message)) {
                enableComponent(component);

                if (currentComponent == null) {
                    switchComponent(componentName);
                } else if (savedLastSelectedComponentName != null
                        && savedLastSelectedComponentName.equals(componentName)) {
                    switchComponent(componentName);
                } else if (savedLastSelectedComponentName == null
                        && currentComponent.getPosition() > component.getPosition()) {
                    switchComponent(componentName);
                }
            }
        }
    }

    private void enableComponent(HttpPanelComponentInterface component) {
        enabledComponents.add(component);
        Collections.sort(enabledComponents, getComponentsComparator());
        if (enabledComponents.size() == 1) {
            toolBarComponents.addSeparator();
            toolBarComponents.addSeparator();
        }
        toolBarComponents.add(component.getButton(), enabledComponents.indexOf(component) + 1);
    }

    private void disableComponent(HttpPanelComponentInterface component) {
        toolBarComponents.remove(component.getButton());
        enabledComponents.remove(component);
        if (enabledComponents.isEmpty()) {
            toolBarComponents.removeAll();
        }
    }

    public void addComponent(
            HttpPanelComponentInterface component, FileConfiguration fileConfiguration) {
        addComponent(component);

        component.setParentConfigurationKey(componentsConfigurationKey);
        component.loadConfig(fileConfiguration);
    }

    public void removeComponent(String componentName) {
        synchronized (components) {
            HttpPanelComponentInterface component = components.get(componentName);
            if (component != null) {
                if (component.isEnabled(message)) {
                    disableComponent(component);
                }

                if (enabledComponents.size() > 0) {
                    switchComponent(enabledComponents.get(0).getName());
                } else {
                    switchEmptyComponent();
                }

                components.remove(componentName);
                panelContent.remove(component.getMainPanel());

                this.validate();
            }
        }
    }

    public void addView(
            String componentName,
            HttpPanelView view,
            Object options,
            FileConfiguration fileConfiguration) {
        synchronized (components) {
            HttpPanelComponentInterface component = components.get(componentName);
            if (component != null) {
                component.addView(view, options, fileConfiguration);
            }
        }
    }

    public void removeView(String componentName, String viewName, Object options) {
        synchronized (components) {
            HttpPanelComponentInterface component = components.get(componentName);
            if (component != null) {
                component.removeView(viewName, options);
            }
        }
    }

    public void addDefaultViewSelector(
            String componentName,
            HttpPanelDefaultViewSelector defaultViewSelector,
            Object options) {
        synchronized (components) {
            HttpPanelComponentInterface component = components.get(componentName);
            if (component != null) {
                component.addDefaultViewSelector(defaultViewSelector, options);
            }
        }
    }

    public void removeDefaultViewSelector(
            String componentName, String defaultViewSelectorName, Object options) {
        synchronized (components) {
            HttpPanelComponentInterface component = components.get(componentName);
            if (component != null) {
                component.removeDefaultViewSelector(defaultViewSelectorName, options);
            }
        }
    }

    public void loadConfig(FileConfiguration fileConfiguration) {
        savedLastSelectedComponentName =
                fileConfiguration.getString(baseConfigurationKey + DEFAULT_COMPONENT_KEY);

        synchronized (components) {
            Iterator<HttpPanelComponentInterface> it = components.values().iterator();
            while (it.hasNext()) {
                it.next().loadConfig(fileConfiguration);
            }

            if (savedLastSelectedComponentName != null
                    && components.containsKey(savedLastSelectedComponentName)) {
                switchComponent(savedLastSelectedComponentName);
            }
        }
    }

    public void saveConfig(FileConfiguration fileConfiguration) {
        if (currentComponent != null) {
            fileConfiguration.setProperty(
                    baseConfigurationKey + DEFAULT_COMPONENT_KEY, currentComponent.getName());
        }

        synchronized (components) {
            Iterator<HttpPanelComponentInterface> it = components.values().iterator();
            while (it.hasNext()) {
                it.next().saveConfig(fileConfiguration);
            }
        }
    }

    private static Comparator<HttpPanelComponentInterface> getComponentsComparator() {
        if (componentsComparator == null) {
            createComponentsComparator();
        }
        return componentsComparator;
    }

    private static synchronized void createComponentsComparator() {
        if (componentsComparator == null) {
            componentsComparator = new ComponentsComparator();
        }
    }

    private static final class ComponentsComparator
            implements Comparator<HttpPanelComponentInterface>, Serializable {

        private static final long serialVersionUID = -1380844848294384189L;

        @Override
        public int compare(HttpPanelComponentInterface o1, HttpPanelComponentInterface o2) {
            final int position1 = o1.getPosition();
            final int position2 = o2.getPosition();
            if (position1 < position2) {
                return -1;
            } else if (position1 > position2) {
                return 1;
            }
            return 0;
        }
    }

    private SwitchComponentItemListener getSwitchComponentItemListener() {
        if (switchComponentItemListener == null) {
            switchComponentItemListener = new SwitchComponentItemListener();
        }
        return switchComponentItemListener;
    }

    private final class SwitchComponentItemListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            String componentName = e.getActionCommand();
            if (isEditable() && !isCurrentComponent(componentName)) {
                try {
                    saveData();
                } catch (InvalidMessageDataException e1) {
                    components.get(componentName).getButton().setSelected(false);

                    StringBuilder warnMessage = new StringBuilder(150);
                    warnMessage.append(
                            Constant.messages.getString("http.panel.component.warn.datainvalid"));

                    String exceptionMessage = e1.getLocalizedMessage();
                    if (exceptionMessage != null && !exceptionMessage.isEmpty()) {
                        warnMessage.append('\n').append(exceptionMessage);
                    }
                    View.getSingleton().showWarningDialog(warnMessage.toString());
                    return;
                }
            }

            switchComponent(componentName);
        }
    }

    public void highlightHeader(SearchMatch sm) {
        if (currentComponent instanceof SearchableHttpPanelComponent) {
            ((SearchableHttpPanelComponent) currentComponent).highlightHeader(sm);
        } else {
            HttpPanelComponentInterface component = findSearchablePanel();
            if (component != null) {
                switchComponent(component.getName());
                ((SearchableHttpPanelComponent) currentComponent).highlightHeader(sm);
            }
        }
    }

    public void highlightBody(SearchMatch sm) {
        if (currentComponent instanceof SearchableHttpPanelComponent) {
            ((SearchableHttpPanelComponent) currentComponent).highlightBody(sm);
        } else {
            HttpPanelComponentInterface component = findSearchablePanel();
            if (component != null) {
                switchComponent(component.getName());
                ((SearchableHttpPanelComponent) currentComponent).highlightBody(sm);
            }
        }
    }

    public void headerSearch(Pattern p, List<SearchMatch> matches) {
        if (currentComponent instanceof SearchableHttpPanelComponent) {
            ((SearchableHttpPanelComponent) currentComponent).searchHeader(p, matches);
        } else {
            HttpPanelComponentInterface component = findSearchablePanel();
            if (component != null) {
                ((SearchableHttpPanelComponent) component).searchHeader(p, matches);
            }
        }
    }

    public void bodySearch(Pattern p, List<SearchMatch> matches) {
        if (currentComponent instanceof SearchableHttpPanelComponent) {
            ((SearchableHttpPanelComponent) currentComponent).searchBody(p, matches);
        } else {
            HttpPanelComponentInterface component = findSearchablePanel();
            if (component != null) {
                ((SearchableHttpPanelComponent) component).searchBody(p, matches);
            }
        }
    }

    private HttpPanelComponentInterface findSearchablePanel() {
        HttpPanelComponentInterface searchableComponent = null;

        synchronized (components) {
            Iterator<HttpPanelComponentInterface> it = components.values().iterator();
            while (it.hasNext()) {
                HttpPanelComponentInterface component = it.next();
                if (component instanceof SearchableHttpPanelComponent) {
                    searchableComponent = component;
                    break;
                }
            }
        }

        return searchableComponent;
    }

    public void addMessagePanelEventListener(MessagePanelEventListener listener) {
        listenerList.add(MessagePanelEventListener.class, listener);
    }

    public void removeMessagePanelEventListener(MessagePanelEventListener listener) {
        listenerList.remove(MessagePanelEventListener.class, listener);
    }

    public void fireMessageViewChangedEvent(HttpPanelView previousView, HttpPanelView currentView) {
        MessageViewSelectedEvent event = null;
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == MessagePanelEventListener.class) {
                if (event == null) {
                    event = new MessageViewSelectedEvent(this, previousView, currentView);
                }
                ((MessagePanelEventListener) listeners[i + 1]).viewSelected(event);
            }
        }
    }

    private void fireComponentChangedEvent(
            HttpPanelComponentInterface previousComponent,
            HttpPanelComponentInterface currentComponent) {
        ComponentChangedEvent event = null;
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == MessagePanelEventListener.class) {
                if (event == null) {
                    event = new ComponentChangedEvent(this, previousComponent, currentComponent);
                }
                ((MessagePanelEventListener) listeners[i + 1]).componentChanged(event);
            }
        }
    }
}
