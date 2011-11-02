package org.springframework.roo.classpath.javaparser;

import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.TypeDeclaration;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.TypeResolutionService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.StringUtils;

@Component(immediate = true)
@Service
public class JavaParserTypeResolutionService implements TypeResolutionService{

	public final JavaType getJavaType(final String fileIdentifier) {
		Assert.hasText(fileIdentifier, "Compilation unit path required");
		Assert.isTrue(new File(fileIdentifier).exists(), "The file doesn't exist");
		Assert.isTrue(new File(fileIdentifier).isFile(), "The identifier doesn't represent a file");
		try {
			File file = new File(fileIdentifier);
			String typeContents = FileUtils.read(file);
			CompilationUnit compilationUnit = JavaParser.parse(new ByteArrayInputStream(typeContents.getBytes()));
			String typeName = fileIdentifier.substring(fileIdentifier.lastIndexOf(File.separator) + 1, fileIdentifier.lastIndexOf("."));
			for (TypeDeclaration typeDeclaration : compilationUnit.getTypes()) {
				if (typeName.equals(typeDeclaration.getName())) {
					return new JavaType(compilationUnit.getPackage().getName().getName() + "." + typeDeclaration.getName());
				}
			}
			return null;
		} catch (ParseException e) {
			throw new IllegalStateException(e);
		}
	}

	public final JavaPackage getPackage(final String fileIdentifier) {
		Assert.hasText(fileIdentifier, "Compilation unit path required");
		Assert.isTrue(new File(fileIdentifier).exists(), "The file doesn't exist");
		Assert.isTrue(new File(fileIdentifier).isFile(), "The identifier doesn't represent a file");
		try {
			File file = new File(fileIdentifier);
			String typeContents = FileUtils.read(file);
			if (!StringUtils.hasText(typeContents)) {
				return null;
			}
			CompilationUnit compilationUnit = JavaParser.parse(new ByteArrayInputStream(typeContents.getBytes()));
			return new JavaPackage(compilationUnit.getPackage().getName().toString());
		} catch (ParseException e) {
			throw new IllegalStateException(e);
		}
	}
}
