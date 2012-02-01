package org.springframework.roo.classpath.javaparser;

import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.TypeDeclaration;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.TypeResolutionService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.FileUtils;

@Component(immediate = true)
@Service
public class JavaParserTypeResolutionService implements TypeResolutionService {

    public final JavaType getJavaType(final String fileIdentifier) {
        Validate.notBlank(fileIdentifier, "Compilation unit path required");
        Validate.isTrue(new File(fileIdentifier).exists(),
                "The file doesn't exist");
        Validate.isTrue(new File(fileIdentifier).isFile(),
                "The identifier doesn't represent a file");
        try {
            final File file = new File(fileIdentifier);
            final String typeContents = FileUtils.read(file);
            final CompilationUnit compilationUnit = JavaParser
                    .parse(new ByteArrayInputStream(typeContents.getBytes()));
            final String typeName = fileIdentifier.substring(
                    fileIdentifier.lastIndexOf(File.separator) + 1,
                    fileIdentifier.lastIndexOf("."));
            for (final TypeDeclaration typeDeclaration : compilationUnit
                    .getTypes()) {
                if (typeName.equals(typeDeclaration.getName())) {
                    return new JavaType(compilationUnit.getPackage().getName()
                            .getName()
                            + "." + typeDeclaration.getName());
                }
            }
            return null;
        }
        catch (final ParseException e) {
            throw new IllegalStateException(e);
        }
    }

    public final JavaPackage getPackage(final String fileIdentifier) {
        Validate.notBlank(fileIdentifier, "Compilation unit path required");
        Validate.isTrue(new File(fileIdentifier).exists(),
                "The file doesn't exist");
        Validate.isTrue(new File(fileIdentifier).isFile(),
                "The identifier doesn't represent a file");
        try {
            final File file = new File(fileIdentifier);
            final String typeContents = FileUtils.read(file);
            if (StringUtils.isBlank(typeContents)) {
                return null;
            }
            final CompilationUnit compilationUnit = JavaParser
                    .parse(new ByteArrayInputStream(typeContents.getBytes()));
            if (compilationUnit == null || compilationUnit.getPackage() == null) {
                return null;
            }
            return new JavaPackage(compilationUnit.getPackage().getName()
                    .toString());
        }
        catch (final ParseException e) {
            throw new IllegalStateException(e);
        }
    }
}
