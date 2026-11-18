package com.mysms.lms.business;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * An LTI 1.1 Basic Outcomes request ({@code imsx_POXEnvelopeRequest}) read safely — no DTD, no external entities: the
 * operation, the result's sourcedId and, for a replace, the score from 0 to 1.
 */
record LtiOutcome(String operation, String sourcedId, BigDecimal score) {

    static final String REPLACE = "replaceResultRequest";

    /** The request, or empty when the body is not a Basic Outcomes request. */
    static Optional<LtiOutcome> parse(byte[] xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(xml));
            Element body = first(document.getDocumentElement(), "imsx_POXBody");
            if (body == null) {
                return Optional.empty();
            }
            Element request = null;
            NodeList children = body.getChildNodes();
            for (int i = 0; i < children.getLength() && request == null; i++) {
                if (children.item(i) instanceof Element element) {
                    request = element;
                }
            }
            if (request == null) {
                return Optional.empty();
            }
            Element sourced = first(request, "sourcedId");
            Element score = first(request, "textString");
            BigDecimal value = score == null ? null : QuizGrader.number(score.getTextContent());
            return Optional.of(new LtiOutcome(request.getLocalName(), sourced == null ? null
                    : sourced.getTextContent().trim(), value));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            return Optional.empty();
        }
    }

    private static Element first(Element root, String localName) {
        NodeList found = root.getElementsByTagNameNS("*", localName);
        return found.getLength() == 0 ? null : (Element) found.item(0);
    }
}
