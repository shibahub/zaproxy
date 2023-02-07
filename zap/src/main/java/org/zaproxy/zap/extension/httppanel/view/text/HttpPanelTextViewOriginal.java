package org.zaproxy.zap.extension.httppanel.view.text;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import org.apache.commons.configuration.FileConfiguration;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.network.HttpMessage;
import org.parosproxy.paros.view.View;
import org.zaproxy.zap.extension.httppanel.Message;
import org.zaproxy.zap.extension.httppanel.view.AbstractStringHttpPanelViewModel;
import org.zaproxy.zap.extension.httppanel.view.HttpPanelView;
import org.zaproxy.zap.extension.httppanel.view.HttpPanelViewModel;
import org.zaproxy.zap.extension.httppanel.view.HttpPanelViewModelEvent;
import org.zaproxy.zap.extension.httppanel.view.HttpPanelViewModelListener;
import org.zaproxy.zap.extension.search.SearchMatch;
import org.zaproxy.zap.extension.search.SearchableHttpPanelView;
import org.zaproxy.zap.view.messagecontainer.http.DefaultSingleHttpMessageContainer;
import org.zaproxy.zap.view.messagecontainer.http.SingleHttpMessageContainer;

public abstract class HttpPanelTextViewOriginal implements HttpPanelView, HttpPanelViewModelListener, SearchableHttpPanelView {

    
    public static final String DEFAULT_MESSAGE_CONTAINER_NAME = "HttpMessagePanel";

    public static final String NAME = "HttpPanelTextViewOriginal";

    private static final String CAPTION_NAME = "Original Request" ;

    private HttpPanelTextArea httpPanelTextArea;
    private JPanel mainPanel;

    private AbstractStringHttpPanelViewModel model;

    private final String messageContainerName;

    public HttpPanelTextViewOriginal(AbstractStringHttpPanelViewModel model) {
        this(DEFAULT_MESSAGE_CONTAINER_NAME, model);
    }

    public HttpPanelTextViewOriginal(String messageContainerName, AbstractStringHttpPanelViewModel model) {
        this.model = model;

        this.messageContainerName = messageContainerName;
        init();

        this.model.addHttpPanelViewModelListener(this);
    }

    private void init() {
        httpPanelTextArea = createHttpPanelTextArea();
        httpPanelTextArea.setEditable(false);
        httpPanelTextArea.setLineWrap(true);
        httpPanelTextArea.setComponentPopupMenu(new CustomPopupMenu());

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(new JScrollPane(httpPanelTextArea), BorderLayout.CENTER);
    }

    protected abstract HttpPanelTextArea createHttpPanelTextArea();

    @Override
    public void setSelected(boolean selected) {
        if (selected) {
            httpPanelTextArea.requestFocusInWindow();
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getCaptionName() {
        return CAPTION_NAME;
    }

    @Override
    public String getTargetViewName() {
        return "";
    }

    @Override
    public int getPosition() {
        return Integer.MIN_VALUE;
    }

    @Override
    public boolean isEnabled(Message msg) {
        return true;
    }

    @Override
    public boolean hasChanged() {
        return true;
    }

    @Override
    public JComponent getPane() {
        return mainPanel;
    }

    @Override
    public boolean isEditable() {
        return httpPanelTextArea.isEditable();
    }

    @Override
    public void setEditable(boolean editable) {
        httpPanelTextArea.setEditable(editable);
        if (!editable) {
            httpPanelTextArea.discardAllEdits();
        }
    }

    @Override
    public HttpPanelViewModel getModel() {
        return model;
    }

    @Override
    public void dataChanged(HttpPanelViewModelEvent e) {
        httpPanelTextArea.setMessage(model.getMessage());

        httpPanelTextArea.setText(model.getData());
        httpPanelTextArea.setCaretPosition(0);
    }

    @Override
    public void save() {
        model.setData(httpPanelTextArea.getText());
    }

    @Override
    public void search(Pattern p, List<SearchMatch> matches) {
        httpPanelTextArea.search(p, matches);
    }

    @Override
    public void highlight(SearchMatch sm) {
        httpPanelTextArea.highlight(sm);
    }

    @Override
    public void setParentConfigurationKey(String configurationKey) {}

    @Override
    public void loadConfiguration(FileConfiguration fileConfiguration) {}

    @Override
    public void saveConfiguration(FileConfiguration fileConfiguration) {}

    protected class CustomPopupMenu extends JPopupMenu {

        private static final long serialVersionUID = 1L;

        @Override
        public void show(Component invoker, int x, int y) {
            if (!httpPanelTextArea.isFocusOwner()) {
                httpPanelTextArea.requestFocusInWindow();
            }

            if (httpPanelTextArea.getMessage() instanceof HttpMessage) {
                SingleHttpMessageContainer messageContainer =
                        new DefaultSingleHttpMessageContainer(
                                messageContainerName,
                                httpPanelTextArea,
                                (HttpMessage) httpPanelTextArea.getMessage());
                View.getSingleton().getPopupMenu().show(messageContainer, x, y);
            } else {
                View.getSingleton().getPopupMenu().show(httpPanelTextArea, x, y);
            }
        }
    }

}
