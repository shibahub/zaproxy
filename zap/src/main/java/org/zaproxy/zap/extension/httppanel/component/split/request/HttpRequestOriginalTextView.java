package org.zaproxy.zap.extension.httppanel.component.split.request;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.zaproxy.zap.extension.httppanel.view.AbstractStringHttpPanelViewModel;
import org.zaproxy.zap.extension.httppanel.view.text.HttpPanelTextArea;
import org.zaproxy.zap.extension.httppanel.view.text.HttpPanelTextView;
import org.zaproxy.zap.extension.httppanel.view.text.HttpPanelTextViewEdited;
import org.zaproxy.zap.extension.httppanel.view.text.HttpPanelTextViewOriginal;
import org.zaproxy.zap.extension.search.SearchMatch;

public class HttpRequestOriginalTextView extends HttpPanelTextViewOriginal{

    public HttpRequestOriginalTextView(AbstractStringHttpPanelViewModel model) {
        super(model);
    }

    @Override
    protected HttpPanelTextArea createHttpPanelTextArea() {
        return new HttpRequestBodyPanelTextArea();
    }
    
    private static class HttpRequestBodyPanelTextArea extends HttpPanelTextArea {

        private static final long serialVersionUID = -5425819266900748512L;

        @Override
        public void search(Pattern p, List<SearchMatch> matches) {
            Matcher m = p.matcher(getText());
            while (m.find()) {
                matches.add(new SearchMatch(SearchMatch.Location.REQUEST_BODY, m.start(), m.end()));
            }
        }

        @Override
        public void highlight(SearchMatch sm) {
            if (!SearchMatch.Location.REQUEST_BODY.equals(sm.getLocation())) {
                return;
            }

            int len = getText().length();
            if (sm.getStart() > len || sm.getEnd() > len) {
                return;
            }

            highlight(sm.getStart(), sm.getEnd());
        }

        
    }
}
