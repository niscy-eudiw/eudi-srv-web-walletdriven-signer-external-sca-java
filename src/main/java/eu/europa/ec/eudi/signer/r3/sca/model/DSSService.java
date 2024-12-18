/*
 Copyright 2024 European Commission

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      https://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package eu.europa.ec.eudi.signer.r3.sca.model;

import eu.europa.ec.eudi.signer.r3.sca.config.TrustedCertificateConfig;
import eu.europa.esig.dss.AbstractSignatureParameters;
import eu.europa.esig.dss.alert.ExceptionOnStatusAlert;
import eu.europa.esig.dss.alert.LogOnStatusAlert;
import eu.europa.esig.dss.asic.cades.ASiCWithCAdESSignatureParameters;
import eu.europa.esig.dss.asic.cades.ASiCWithCAdESTimestampParameters;
import eu.europa.esig.dss.asic.cades.signature.ASiCWithCAdESService;
import eu.europa.esig.dss.asic.xades.ASiCWithXAdESSignatureParameters;
import eu.europa.esig.dss.asic.xades.signature.ASiCWithXAdESService;
import eu.europa.esig.dss.cades.CAdESSignatureParameters;
import eu.europa.esig.dss.cades.signature.CAdESService;
import eu.europa.esig.dss.cades.signature.CAdESTimestampParameters;
import eu.europa.esig.dss.enumerations.*;
import eu.europa.esig.dss.model.*;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.pades.*;
import eu.europa.esig.dss.pdf.pdfbox.PdfBoxNativeObjectFactory;
import eu.europa.esig.dss.pdf.pdfbox.visible.PdfBoxNativeFont;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.xades.XAdESSignatureParameters;
import eu.europa.esig.dss.xades.XAdESTimestampParameters;
import eu.europa.esig.dss.xades.signature.XAdESService;
import eu.europa.esig.dss.jades.JAdESSignatureParameters;
import eu.europa.esig.dss.jades.JAdESTimestampParameters;
import eu.europa.esig.dss.jades.signature.JAdESService;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.service.SecureRandomNonceSource;
import eu.europa.esig.dss.service.crl.OnlineCRLSource;
import eu.europa.esig.dss.service.http.commons.CommonsDataLoader;
import eu.europa.esig.dss.service.http.commons.OCSPDataLoader;
import eu.europa.esig.dss.service.http.commons.TimestampDataLoader;
import eu.europa.esig.dss.service.ocsp.OnlineOCSPSource;
import eu.europa.esig.dss.service.tsp.OnlineTSPSource;
import eu.europa.esig.dss.signature.DocumentSignatureService;
import eu.europa.esig.dss.signature.MultipleDocumentsSignatureService;
import eu.europa.esig.dss.spi.x509.CommonTrustedCertificateSource;
import eu.europa.esig.dss.validation.CertificateVerifier;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.validation.OCSPFirstRevocationDataLoadingStrategyFactory;
import eu.europa.esig.dss.validation.RevocationDataVerifier;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;

import javax.imageio.ImageIO;

import static eu.europa.esig.dss.enumerations.SignatureForm.PAdES;

@Service
public class DSSService {
    private final static Logger logger = LogManager.getLogger(DSSService.class);
	private final TrustedCertificateConfig trustedCertificateConfig;

	public DSSService(@Autowired TrustedCertificateConfig trustedCertificateConfig){
		this.trustedCertificateConfig = trustedCertificateConfig;
	}

    public static SignatureLevel checkConformance_level(String conformance_level, String string) {
        String enumValue = mapToEnumValue(conformance_level, string);
        if (enumValue == null) {
            return null;
        }
        try {
            return SignatureLevel.valueByName(enumValue);
        } catch (IllegalArgumentException e) {
            logger.error("Session_id:{}. Error message: {}.", RequestContextHolder.currentRequestAttributes().getSessionId(), e.getMessage());
        }

        return null;
    }

    private static String mapToEnumValue(String conformance_level, String string) {
        String prefix;
        switch (string) {
            case "P":
                prefix = "PAdES_BASELINE_";
                break;
            case "C":
                prefix = "CAdES_BASELINE_";
                break;
            case "J":
                prefix = "JAdES_BASELINE_";
                break;
            case "X":
                prefix = "XAdES_BASELINE_";
                break;
            default:
				logger.error("Session_id:{},Conformance Level invalid.", RequestContextHolder.currentRequestAttributes().getSessionId());
                return null;
        }

		return switch (conformance_level) {
			case "Ades-B-B" -> prefix + "B";
			case "Ades-B-LT" -> prefix + "LT";
			case "Ades-B-LTA" -> prefix + "LTA";
			case "Ades-B-T" -> prefix + "T";
			default -> {
				logger.error("Session_id:" + RequestContextHolder.currentRequestAttributes().getSessionId() + "," + "Conformance Level invalid.");
				yield null;
			}
		};
    }

    public static SignatureForm checkSignForm(String signForm) {
		return switch (signForm) {
			case "P" -> SignatureForm.PAdES;
			case "C" -> SignatureForm.CAdES;
			case "J" -> SignatureForm.JAdES;
			case "X" -> SignatureForm.XAdES;
			default -> {
				logger.error("Session_id:{},Signature Format invalid.", RequestContextHolder.currentRequestAttributes().getSessionId());
				yield null;
			}
		};
    }

    public static DigestAlgorithm checkDigestAlgorithm(String alg) throws Exception{
		try {
			return DigestAlgorithm.forOID(alg);
		}
		catch (IllegalArgumentException e){
			logger.info("Session_id:{}, hashAlgorithmOID given doesn't match a DigestAlgorithm. Try to load SignatureAlgorithm.", RequestContextHolder.currentRequestAttributes().getSessionId());

			try {
				SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.forOID(alg);
				return signatureAlgorithm.getDigestAlgorithm();
			}
			catch (IllegalArgumentException e1){
				logger.error("Session_id:{},Signature Digest Algorithm invalid.", RequestContextHolder.currentRequestAttributes().getSessionId());
				throw new Exception("A DigestAlgorithm was not found from the hashAlgorithmOID.");
			}
		}
    }

    public static ASiCContainerType checkASiCContainerType(String alg) {
		return switch (alg) {
			case "No" -> null;
			case "ASiC-E" -> ASiCContainerType.ASiC_E;
			case "ASiC-S" -> ASiCContainerType.ASiC_S;
			default -> {
				logger.error("Session_id:{},ASICC Container Type invalid.", RequestContextHolder.currentRequestAttributes().getSessionId());
				yield null;
			}
		};
    }

    public static SignaturePackaging checkEnvProps(String env) {
		return switch (env) {
			case "ENVELOPED" -> SignaturePackaging.ENVELOPED;
			case "ENVELOPING" -> SignaturePackaging.ENVELOPING;
			case "DETACHED" -> SignaturePackaging.DETACHED;
			case "INTERNALLY_DETACHED" -> SignaturePackaging.INTERNALLY_DETACHED;
			default -> {
				logger.error("Session_id:{},Signature Packaging invalid.", RequestContextHolder.currentRequestAttributes().getSessionId());
				yield null;
			}
		};
    }

    public DSSDocument loadDssDocument(String document) {
        byte[] dataDocument = Base64.getDecoder().decode(document);
        return new InMemoryDocument(dataDocument);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    /*
     * Function that returns the digest of the data to be signed from the document and parameters received
     */
    public byte[] getDigestOfDataToBeSigned(SignatureDocumentForm form) throws IOException {
        DocumentSignatureService service = getSignatureService(form.getContainerType(), form.getSignatureForm(), form.getTrustedCertificates());
		logger.info("Session_id:{},DataToBeSignedData Service created.", RequestContextHolder.currentRequestAttributes().getSessionId());

        AbstractSignatureParameters parameters = fillParameters(form);
		logger.info("Session_id:{},DataToBeSignedData Parameters Filled.", RequestContextHolder.currentRequestAttributes().getSessionId());

        DSSDocument toSignDocument = form.getDocumentToSign();

        ToBeSigned toBeSigned;
		if(form.getSignatureForm().equals(SignatureForm.PAdES)) {
			PAdESService s = (PAdESService) service;
			PAdESSignatureParameters p = (PAdESSignatureParameters) parameters;
			SignatureImageParameters imageParameters = setVisualSignature(toSignDocument, form.getCertificate(), form.getDate());
			p.setImageParameters(imageParameters);
			s.setPdfObjFactory(new PdfBoxNativeObjectFactory());
			toBeSigned = service.getDataToSign(toSignDocument, parameters);
		}
		else toBeSigned = service.getDataToSign(toSignDocument, parameters);


		return DSSUtils.digest(parameters.getDigestAlgorithm(), toBeSigned.getBytes());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public DSSDocument signDocument(SignatureDocumentForm form) throws IOException {

        DocumentSignatureService service = getSignatureService(form.getContainerType(), form.getSignatureForm(), form.getTrustedCertificates());
		logger.info("Session_id:{}, signDocument Service created.", RequestContextHolder.currentRequestAttributes().getSessionId());

        AbstractSignatureParameters parameters = fillParameters(form);
		logger.info("Session_id:{}, DataToBeSignedData Parameters Filled.", RequestContextHolder.currentRequestAttributes().getSessionId());

        DSSDocument toSignDocument = form.getDocumentToSign();

        SignatureValue signatureValue = new SignatureValue();
        signatureValue.setAlgorithm(SignatureAlgorithm.getAlgorithm(form.getEncryptionAlgorithm(), form.getDigestAlgorithm()));
        signatureValue.setValue(form.getSignatureValue());

		if(form.getSignatureForm().equals(SignatureForm.PAdES)) {
			PAdESService s = (PAdESService) service;
			PAdESSignatureParameters p = (PAdESSignatureParameters) parameters;
			SignatureImageParameters imageParameters = setVisualSignature(toSignDocument, form.getCertificate(), form.getDate());
			p.setImageParameters(imageParameters);
			s.setPdfObjFactory(new PdfBoxNativeObjectFactory());
			return s.signDocument(toSignDocument, p, signatureValue);
		}
		else return service.signDocument(toSignDocument, parameters, signatureValue);
    }

	private SignatureImageParameters setVisualSignature(DSSDocument document, X509Certificate certificate, Date date) throws IOException {

		InputStream is = document.openStream();
		PDDocument pdfDocument = PDDocument.load(is, (String) null);

		// Initialize visual signature and configure
		SignatureImageParameters imageParameters = createImageParameters();

		SignatureImageTextParameters textParameters = createTextParameters(certificate, date);
		imageParameters.setTextParameters(textParameters);

		SignatureFieldParameters fieldParameters = createFieldRectangle(pdfDocument);
		imageParameters.setFieldParameters(fieldParameters);

		return imageParameters;
	}



	private SignatureFieldParameters createFieldRectangle(PDDocument pdfDocument) {
		SignatureFieldParameters fieldParameters = new SignatureFieldParameters();
		// the origin is the left and top corner of the page
		fieldParameters.setOriginX(20);
		fieldParameters.setOriginY(20);
		fieldParameters.setHeight(50);
		fieldParameters.setPage(pdfDocument.getNumberOfPages());
		fieldParameters.setRotation(VisualSignatureRotation.AUTOMATIC);
		return fieldParameters;
	}

	private SignatureImageTextParameters createTextParameters(X509Certificate certificate, Date date){
		X500Name x500Name = new X500Name(certificate.getSubjectX500Principal().getName());
		RDN cn = x500Name.getRDNs(BCStyle.CN)[0];
		String name = IETFUtils.valueToString(cn.getFirst().getValue());

		SimpleDateFormat sdf = new SimpleDateFormat("EEEE dd MMM yyyy, HH:mm:ss z");
		String dateString = sdf.format(date.getTime());

		SignatureImageTextParameters textParameters = new SignatureImageTextParameters();
		DSSFont font = new PdfBoxNativeFont(PDType1Font.HELVETICA);
		font.setSize(10);
		textParameters.setFont(font);
		textParameters.setText("Signing documents with EUDI Wallet\nSigner: "+name+"\n"+dateString);
		textParameters.setTextColor(Color.BLACK);
		textParameters.setPadding(10); // Defines a padding between the text and a border of its bounding area
		textParameters.setTextWrapping(TextWrapping.FONT_BASED); // TextWrapping parameter allows defining the text wrapping behavior within  the signature field
		textParameters.setSignerTextPosition(SignerTextPosition.RIGHT); // the text will be placed on the right side and the image on the right
		textParameters.setSignerTextHorizontalAlignment(SignerTextHorizontalAlignment.LEFT); // Specifies a horizontal alignment of a text with respect to its area
		textParameters.setSignerTextVerticalAlignment(SignerTextVerticalAlignment.TOP); // Specifies a vertical alignment of a text block with respect to a signature field area
		return textParameters;
	}

	private SignatureImageParameters createImageParameters() throws IOException {
		SignatureImageParameters imageParameters = new SignatureImageParameters();

		// Visible signature positioning
		imageParameters.setAlignmentHorizontal(VisualSignatureAlignmentHorizontal.LEFT);
		imageParameters.setAlignmentVertical(VisualSignatureAlignmentVertical.BOTTOM);
		imageParameters.setImageScaling(ImageScaling.ZOOM_AND_CENTER);
		imageParameters.setImage(new InMemoryDocument(getClass().getResourceAsStream("/img/symbol.png")));

		return imageParameters;
	}

    @SuppressWarnings({ "rawtypes" })
    private AbstractSignatureParameters fillParameters(SignatureDocumentForm form) {

        AbstractSignatureParameters parameters = getSignatureParameters(form.getContainerType(),  form.getSignatureForm());
        parameters.setSignaturePackaging(form.getSignaturePackaging());

        fillParameters(parameters, form);

        return parameters;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void fillParameters(AbstractSignatureParameters parameters, SignatureDocumentForm form) {

        parameters.setSignatureLevel(form.getSignatureLevel());
        parameters.setDigestAlgorithm(form.getDigestAlgorithm());
        parameters.bLevel().setSigningDate(form.getDate());

        CertificateToken signingCertificate = new CertificateToken(form.getCertificate());
        parameters.setSigningCertificate(signingCertificate);

        List<X509Certificate> certificateChainBytes = form.getCertChain();
        List<CertificateToken> certChainToken = new ArrayList<>();
        for (X509Certificate cert : certificateChainBytes) {
            certChainToken.add(new CertificateToken(cert));
        }
        parameters.setCertificateChain(certChainToken);

        fillTimestampParameters(parameters, form);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void fillTimestampParameters(AbstractSignatureParameters parameters, SignatureDocumentForm form) {
        SignatureForm signatureForm = form.getSignatureForm();
        ASiCContainerType containerType = form.getContainerType();

        TimestampParameters timestampParameters = getTimestampParameters(containerType, signatureForm);
        timestampParameters.setDigestAlgorithm(form.getDigestAlgorithm());

        parameters.setContentTimestampParameters(timestampParameters);
        parameters.setSignatureTimestampParameters(timestampParameters);
        parameters.setArchiveTimestampParameters(timestampParameters);
    }

    @SuppressWarnings("rawtypes")
    private DocumentSignatureService getSignatureService(ASiCContainerType containerType, SignatureForm signatureForm, CommonTrustedCertificateSource TrustedCertificates) {

        CertificateVerifier cv = new CommonCertificateVerifier();

        cv.setTrustedCertSources(TrustedCertificates);
        cv.setCheckRevocationForUntrustedChains(false);

        OnlineCRLSource onlineCRLSource = new OnlineCRLSource();
        onlineCRLSource.setDataLoader(new CommonsDataLoader());
        cv.setCrlSource(onlineCRLSource);

        OnlineOCSPSource onlineOCSPSource = new OnlineOCSPSource();
        onlineOCSPSource.setDataLoader(new OCSPDataLoader());
        onlineOCSPSource.setNonceSource(new SecureRandomNonceSource());
        cv.setOcspSource(null);

        // Capability to download resources from AIA
        cv.setAIASource(null);

        // cv.setDefaultDigestAlgorithm(DigestAlgorithm.SHA256);

        cv.setAlertOnMissingRevocationData(new ExceptionOnStatusAlert());

        cv.setAlertOnUncoveredPOE(new LogOnStatusAlert(Level.WARN));

        cv.setAlertOnRevokedCertificate(new ExceptionOnStatusAlert());

        cv.setAlertOnInvalidTimestamp(new ExceptionOnStatusAlert());

        cv.setAlertOnNoRevocationAfterBestSignatureTime(new LogOnStatusAlert(Level.ERROR));

        cv.setAlertOnExpiredSignature(new ExceptionOnStatusAlert());

        cv.setRevocationDataLoadingStrategyFactory(new OCSPFirstRevocationDataLoadingStrategyFactory());

        RevocationDataVerifier revocationDataVerifier = RevocationDataVerifier.createDefaultRevocationDataVerifier();
        cv.setRevocationDataVerifier(revocationDataVerifier);

        cv.setRevocationFallback(true);

        DocumentSignatureService service;
        if (containerType != null) {
            service = (DocumentSignatureService) getASiCSignatureService(signatureForm, cv);
        } else {
			service = switch (signatureForm) {
				case CAdES -> new CAdESService(cv);
				case PAdES -> new PAdESService(cv);
				case XAdES -> new XAdESService(cv);
				case JAdES -> new JAdESService(cv);
				default ->
					  throw new IllegalArgumentException(String.format("Unknown signature form : %s", signatureForm));
			};
        }

        String tspServer = this.trustedCertificateConfig.getTimeStampAuthority();
        OnlineTSPSource onlineTSPSource = new OnlineTSPSource(tspServer);
        onlineTSPSource.setDataLoader(new TimestampDataLoader());
        service.setTspSource(onlineTSPSource);
        return service;
    }

    @SuppressWarnings("rawtypes")
    private MultipleDocumentsSignatureService getASiCSignatureService(SignatureForm signatureForm, CertificateVerifier cv) {
		return switch (signatureForm) {
			case CAdES -> new ASiCWithCAdESService(cv);
			case XAdES -> new ASiCWithXAdESService(cv);
			default -> throw new IllegalArgumentException(
				  String.format("Not supported signature form for an ASiC container : %s", signatureForm));
		};
    }

    private TimestampParameters getTimestampParameters(ASiCContainerType containerType, SignatureForm signatureForm) {
        TimestampParameters parameters;
        if (containerType == null) {
			parameters = switch (signatureForm) {
				case CAdES -> new CAdESTimestampParameters();
				case XAdES -> new XAdESTimestampParameters();
				case PAdES -> new PAdESTimestampParameters();
				case JAdES -> new JAdESTimestampParameters();
				default -> throw new IllegalArgumentException(
					  String.format("Not supported signature form for a time-stamp : %s", signatureForm));
			};

        } else {
            switch (signatureForm) {
                case CAdES:
                    ASiCWithCAdESTimestampParameters asicParameters = new ASiCWithCAdESTimestampParameters();
                    asicParameters.aSiC().setContainerType(containerType);
                    parameters = asicParameters;
                    break;
                case XAdES:
                    parameters = new XAdESTimestampParameters();
                    break;
                default:
                    throw new IllegalArgumentException(
                          String.format("Not supported signature form for an ASiC time-stamp : %s", signatureForm));
            }
        }
        return parameters;
    }

    @SuppressWarnings({ "rawtypes" })
    private AbstractSignatureParameters getSignatureParameters(ASiCContainerType containerType, SignatureForm signatureForm) {
        AbstractSignatureParameters parameters;
        if (containerType != null) {
            parameters = getASiCSignatureParameters(containerType, signatureForm);
        } else {
            switch (signatureForm) {
                case CAdES:
                    parameters = new CAdESSignatureParameters();
                    break;
                case PAdES:
                    PAdESSignatureParameters padesParams = new PAdESSignatureParameters();
                    padesParams.setContentSize(9472 * 2); // double reserved space for signature
                    parameters = padesParams;
                    break;
                case XAdES:
                    parameters = new XAdESSignatureParameters();
                    break;
                case JAdES:
                    JAdESSignatureParameters jadesParameters = new JAdESSignatureParameters();
                    jadesParameters.setJwsSerializationType(JWSSerializationType.JSON_SERIALIZATION); // to allow T+
                    // levels +
                    // parallel
                    // signing
                    jadesParameters.setSigDMechanism(SigDMechanism.OBJECT_ID_BY_URI_HASH); // to use by default
                    parameters = jadesParameters;
                    break;
                default:
                    throw new IllegalArgumentException(String.format("Unknown signature form : %s", signatureForm));
            }
        }
        return parameters;
    }

    @SuppressWarnings({ "rawtypes" })
    private AbstractSignatureParameters getASiCSignatureParameters(ASiCContainerType containerType, SignatureForm signatureForm) {
        AbstractSignatureParameters parameters;
        switch (signatureForm) {
            case CAdES:
                ASiCWithCAdESSignatureParameters asicCadesParams = new ASiCWithCAdESSignatureParameters();
                asicCadesParams.aSiC().setContainerType(containerType);
                parameters = asicCadesParams;
                break;
            case XAdES:
                ASiCWithXAdESSignatureParameters asicXadesParams = new ASiCWithXAdESSignatureParameters();
                asicXadesParams.aSiC().setContainerType(containerType);
                parameters = asicXadesParams;
                break;
            default:
                throw new IllegalArgumentException(
                      String.format("Not supported signature form for an ASiC container : %s", signatureForm));
        }
        return parameters;
    }
}