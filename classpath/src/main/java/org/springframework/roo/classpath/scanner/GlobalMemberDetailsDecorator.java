package org.springframework.roo.classpath.scanner;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.IdentifiableJavaStructure;
import org.springframework.roo.classpath.details.ImportMetadata;
import org.springframework.roo.classpath.details.ImportMetadataBuilder;
import org.springframework.roo.classpath.details.InitializerMetadata;
import org.springframework.roo.classpath.details.InitializerMetadataBuilder;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetailsBuilder;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.model.AbstractCustomDataAccessorBuilder;
import org.springframework.roo.model.CustomData;

/**
 * A {@link MemberDetailsDecorator} that is capable of applying {@link GlobalCustomDataRequest} objects
 * to the target {@link IdentifiableJavaStructure}, provided the target {@link IdentifiableJavaStructure}
 * is located within the {@link MemberDetails} presented to the decorator instance.
 * 
 * @author Ben Alex
 * @author Alan Stewart
 * @since 1.1.3
 */
@Service
@Component
public class GlobalMemberDetailsDecorator implements MemberDetailsDecorator {
	public static final String GLOBAL_MEMBER_DETAILS_DECORATOR_TAG = "GLOBAL_MEMBER_DETAILS_DECORATOR_TAG";
	
	public MemberDetails decorate(String requestingClass, MemberDetails memberDetails) {
		List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList = new ArrayList<MemberHoldingTypeDetails>(memberDetails.getDetails());
		
		// Locate any requests that we add custom data to identifiable java structures
		GlobalCustomDataRequest allRequests = new GlobalCustomDataRequest();
		for (MemberHoldingTypeDetails memberHoldingTypeDetails: memberDetails.getDetails()) {
			GlobalCustomDataRequest currentRequest = (GlobalCustomDataRequest) memberHoldingTypeDetails.getCustomData().get(GLOBAL_MEMBER_DETAILS_DECORATOR_TAG);	
			if (currentRequest != null) {
				allRequests.addAll(currentRequest);
			}
		}
		
		// Apply them
		for (IdentifiableJavaStructure javaStructure : allRequests) {
			CustomData dataToApply = allRequests.getCustomDataFor(javaStructure);
			for (MemberHoldingTypeDetails memberHoldingTypeDetails: memberDetails.getDetails()) {
				if (!memberHoldingTypeDetails.getDeclaredByMetadataId().equals(javaStructure.getDeclaredByMetadataId())) {
					continue;
				}

				IdentifiableJavaStructure newJavaStructure = decorateIfNeeded(javaStructure, dataToApply, memberHoldingTypeDetails.getDeclaredByMetadataId());
				// TODO Detect what sort of IdentifiableJavaStructure newJavaStructure is and either create a new structure if a ClassOrInterfaceTypeDetails
				// or ItdTypeDetails and set the existing fields, methods etc then replace the existing
				// MemberHoldingTypeDetails with the new one, or if the  IdentifiableJavaStructure is a FieldMetadata instance for example,
				// update the fields in the current MemberHoldingTypeDetails and replace also.
				// This can be done in the decorateIfNeeded method below to avoid detection duplication here.
				
				
					
			}
		}
		
		return new MemberDetailsImpl(memberHoldingTypeDetailsList);
	}
	

	private IdentifiableJavaStructure decorateIfNeeded(IdentifiableJavaStructure javaStructure, CustomData dataToApply, String targetMid) {
		AbstractCustomDataAccessorBuilder<?> builder = null;
		if (javaStructure instanceof ClassOrInterfaceTypeDetails) {
			builder = new ClassOrInterfaceTypeDetailsBuilder((ClassOrInterfaceTypeDetails) javaStructure);
			System.out.println("created ClassOrInterfaceTypeDetailsBuilder for " + targetMid);
		} else if (javaStructure instanceof ItdTypeDetails) {
			builder = new ItdTypeDetailsBuilder((ItdTypeDetails) javaStructure);
			System.out.println("created ItdTypeDetailsBuilder for " + targetMid);
		} else if (javaStructure instanceof ConstructorMetadata) {
			builder = new ConstructorMetadataBuilder((ConstructorMetadata) javaStructure);
		} else if (javaStructure instanceof MethodMetadata) {
			builder = new MethodMetadataBuilder((MethodMetadata) javaStructure);
			System.out.println("created MethodMetadataBuilder for " + targetMid);
		} else if (javaStructure instanceof FieldMetadata) {
			builder = new FieldMetadataBuilder((FieldMetadata) javaStructure);
			System.out.println("created FieldMetadataBuilder for " + targetMid);
		} else if (javaStructure instanceof ImportMetadata) {
			builder = new ImportMetadataBuilder((ImportMetadata) javaStructure);
		} else if (javaStructure instanceof InitializerMetadata) {
			builder = new InitializerMetadataBuilder((InitializerMetadata) javaStructure);
		} else {
			throw new IllegalStateException("Target MID '" + targetMid + "' for custom data '" + dataToApply + "' is of an unsupported Java structure of type '" + javaStructure.getClass().getName() + "'");
		}
		
		// We have a builder, so we can mess with the custom data
		builder.getCustomData().append(dataToApply);
		return (IdentifiableJavaStructure) builder.build();
	}
}
