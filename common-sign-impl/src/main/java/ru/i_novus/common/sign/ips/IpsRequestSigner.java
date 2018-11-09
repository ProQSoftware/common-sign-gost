package ru.i_novus.common.sign.ips;

import org.apache.xml.security.c14n.CanonicalizationException;
import org.apache.xml.security.c14n.InvalidCanonicalizerException;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Node;
import ru.i_novus.common.sign.GostXmlSignature;
import ru.i_novus.common.sign.api.SignAlgorithmType;
import ru.i_novus.common.sign.util.CryptoFormatConverter;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.UUID;

import static ru.i_novus.common.sign.GostXmlSignature.DS_NS;
import static ru.i_novus.common.sign.GostXmlSignature.WSSE_NS;
import static ru.i_novus.common.sign.GostXmlSignature.WSU_NS;

public final class IpsRequestSigner {
    private static final String WSA_NS = "http://www.w3.org/2005/08/addressing";
    private static final String EGISZ_NS = "http://egisz.rosminzdrav.ru";
    private static final String EGISZ_PREFIX = "egisz";

    private IpsRequestSigner() {
        // не позволяет создать экземпляр класса, класс утилитный
    }

    /**
     * Подписывает SOAP-запрос для сервиса ИПС
     *
     * @param message сообщение
     * @param soapService адрес сервиса в ИПС
     * @param soapAction действие сервиса
     * @param clientEntityId идентификатор системы
     * @param encodedCertificate сертификат в формате PEM
     * @param encodedPrivateKey закрытый ключ в формате PEM
     */
    public static void signIpsRequest(SOAPMessage message, String soapService, String soapAction, String clientEntityId,
                                      String encodedCertificate, String encodedPrivateKey) throws
            SOAPException, GeneralSecurityException, TransformerException, InvalidCanonicalizerException, CanonicalizationException, IOException {

        CryptoFormatConverter converter = CryptoFormatConverter.getInstance();
        X509Certificate x509Certificate = converter.getCertificateFromPEMEncoded(encodedCertificate);
        PrivateKey privateKey = converter.getPKFromPEMEncoded(SignAlgorithmType.findByCertificate(x509Certificate), encodedPrivateKey);
        signIpsRequest(message, soapService, soapAction, clientEntityId, privateKey, x509Certificate);
    }

    /**
     * Подписывает SOAP-запрос для сервиса ИПС
     *
     * @param message сообщение
     * @param soapService адрес сервиса в ИПС
     * @param soapAction действие сервиса
     * @param clientEntityId идентификатор системы
     * @param certificate сертификат в формате
     * @param privateKey закрытый ключ в формате {@link java.security.PrivateKey}
     */
    public static void signIpsRequest(SOAPMessage message, String soapService, String soapAction, String clientEntityId,
                                      PrivateKey privateKey, X509Certificate certificate) throws
            SOAPException, GeneralSecurityException, TransformerException, InvalidCanonicalizerException, CanonicalizationException, IOException {
        // Добавляем требуемые пространства имен
        message.getSOAPPart().getEnvelope().addNamespaceDeclaration("wsse", WSSE_NS)
                .addNamespaceDeclaration("wsu", WSU_NS)
                .addNamespaceDeclaration("ds", DS_NS)
                .addNamespaceDeclaration("wsa", WSA_NS);
        // Проставляем идентификатор для элемента Body
        message.getSOAPBody().setAttribute("wsu:Id", "body");
        // Добавляем элементы transportHeader, authInfo и clientEntityId
        Node transportHeader = XPathAPI.selectSingleNode(message.getSOAPHeader(),
                "//*[local-name()='transportHeader']/*[local-name()='authInfo']/*[local-name()='clientEntityId']");
        if (transportHeader == null) {
            message.getSOAPHeader().addChildElement(new QName(EGISZ_NS, "transportHeader", EGISZ_PREFIX))
                    .addChildElement("authInfo", EGISZ_PREFIX)
                    .addChildElement("clientEntityId", EGISZ_PREFIX)
                    .addTextNode(clientEntityId);
        }
        // Добавляем элементы MessageID, Action и To
        Node messageId = XPathAPI.selectSingleNode(message.getSOAPHeader(), "//*[local-name()='MessageID']");
        if (messageId == null) {
            message.getSOAPHeader().addChildElement("MessageID", "wsa").addTextNode(UUID.randomUUID().toString());
        }
        Node action = XPathAPI.selectSingleNode(message.getSOAPHeader(), "//*[local-name()='Action']");
        if (action == null) {
            message.getSOAPHeader().addChildElement("Action", "wsa").addTextNode(soapAction);
        }
        Node to = XPathAPI.selectSingleNode(message.getSOAPHeader(), "//*[local-name()='To']");
        if (to == null) {
            message.getSOAPHeader().addChildElement("To", "wsa").addTextNode(soapService);
        }
        SignAlgorithmType signAlgorithmType = SignAlgorithmType.findByAlgorithmName(certificate.getSigAlgName());
        // Добавляем элемент Security
        GostXmlSignature.addSecurityElement(message, certificate, null);
        // Подписываем сообщение
        GostXmlSignature.sign(message, privateKey, signAlgorithmType);
    }

    /**
     * Подписывает SOAP-ответ для сервиса ИПС
     *
     * @param message сообщение
     * @param encodedCertificate сертификат в формате PEM
     * @param encodedKey закрытый ключ в формате PEM
     */
    public static void signIpsResponse(SOAPMessage message, String encodedCertificate, String encodedKey) throws SOAPException,
            TransformerException, GeneralSecurityException, InvalidCanonicalizerException, CanonicalizationException, IOException {
        CryptoFormatConverter converter = CryptoFormatConverter.getInstance();
        X509Certificate certificate = converter.getCertificateFromPEMEncoded(encodedCertificate);
        signIpsResponse(message, converter.getPKFromPEMEncoded(SignAlgorithmType.findByCertificate(certificate), encodedKey), certificate);
    }

    /**
     * Подписывает SOAP-ответ для сервиса ИПС
     * @param message сообщение
     * @param privateKey закрытый ключ в формате PEM
     * @param certificate сертификат в формате PEM
     */
    public static void signIpsResponse(SOAPMessage message, PrivateKey privateKey, X509Certificate certificate) throws SOAPException,
            TransformerException, GeneralSecurityException, InvalidCanonicalizerException, CanonicalizationException, IOException {
        // Добавляем требуемые пространства имен
        message.getSOAPPart().getEnvelope().addNamespaceDeclaration("wsse", WSSE_NS)
                .addNamespaceDeclaration("wsu", WSU_NS)
                .addNamespaceDeclaration("ds", DS_NS)
                .addNamespaceDeclaration("wsa", WSA_NS);
        // Проставляем идентификатор для элемента Body
        message.getSOAPBody().setAttribute("wsu:Id", "body");
        // Добавляем элементы MessageID
        Node messageId = XPathAPI.selectSingleNode(message.getSOAPHeader(), "//*[local-name()='MessageID']");
        if (messageId == null) {
            message.getSOAPHeader().addChildElement("MessageID", "wsa").addTextNode(UUID.randomUUID().toString());
        }
        // Добавляем элемент Security
        GostXmlSignature.addSecurityElement(message, certificate, null);
        // Подписываем сообщение
        GostXmlSignature.sign(message, privateKey, SignAlgorithmType.findByCertificate(certificate));
    }
}