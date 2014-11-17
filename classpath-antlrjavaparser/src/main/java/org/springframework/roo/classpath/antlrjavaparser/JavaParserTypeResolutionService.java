package org.springframework.roo.classpath.antlrjavaparser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.TypeResolutionService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;

import com.github.antlrjavaparser.JavaParser;
import com.github.antlrjavaparser.ParseException;
import com.github.antlrjavaparser.api.CompilationUnit;
import com.github.antlrjavaparser.api.body.TypeDeclaration;

@Component
@Service
public class JavaParserTypeResolutionService implements TypeResolutionService {

    @Override
    public final JavaType getJavaType(final String fileIdentifier) {
        Validate.notBlank(fileIdentifier, "Compilation unit path required");
        Validate.isTrue(new File(fileIdentifier).exists(),
                "The file doesn't exist");
        Validate.isTrue(new File(fileIdentifier).isFile(),
                "The identifier doesn't represent a file");
        try {
            final File file = new File(fileIdentifier);
            String typeContents = "";
            try {
                typeContents = FileUtils.readFileToString(file);
            }
            catch (final IOException ignored) {
            }
            if (StringUtils.isBlank(typeContents)) {
                return null;
            }
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
        catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        catch (final ParseException e) {
            throw new IllegalStateException("Failed to parse " + fileIdentifier
                    + " : " + e.getMessage());
        }
    }

    @Override
    public final JavaPackage getPackage(final String fileIdentifier) {
        Validate.notBlank(fileIdentifier, "Compilation unit path required");
        Validate.isTrue(new File(fileIdentifier).exists(),
                "The file doesn't exist");
        Validate.isTrue(new File(fileIdentifier).isFile(),
                "The identifier doesn't represent a file");
        try {
            final File file = new File(fileIdentifier);
            String typeContents = "";
            try {
                typeContents = FileUtils.readFileToString(file);
            }
            catch (final IOException ignored) {
            }
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
        catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        catch (final ParseException e) {
            throw new IllegalStateException("Failed to parse " + fileIdentifier
                    + " : " + e.getMessage());
        }
    }
}
