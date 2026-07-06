package com.theatomicity.aop.ut.generator.utils;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.Name;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GeneratorUtils {

    public static final String MOCK_OBJECT_PATTERN = "mock(%s.class)";

    public ClassOrInterfaceDeclaration getMainType(final CompilationUnit originCu) {
        return (ClassOrInterfaceDeclaration) originCu.getType(0);
    }

    public String getInstanceName(final String originClassName) {
        return Character.toLowerCase(originClassName.charAt(0)) + originClassName.substring(1);
    }

    public String getOriginFullClassName(final CompilationUnit originCu) {
        final ClassOrInterfaceDeclaration originClass = this.getMainType(originCu);
        final String classNameAsString = originClass.getNameAsString();
        final String packageNameAsString = originCu.getPackageDeclaration()
                .map(PackageDeclaration::getName)
                .map(Name::asString)
                .orElse("");
        return String.format("%s.%s", packageNameAsString, classNameAsString);
    }

    public void addImportIfNotExists(final CompilationUnit utCu, final String importName, final Boolean isStatic,
                                     final Boolean isAsterisk) {
        final boolean alreadyExists = utCu.getImports().stream()
                .anyMatch(importDeclaration -> this.matchesImport(importDeclaration, importName, isStatic, isAsterisk));
        final boolean isImmutableCollection = List.of("ImmutableCollections$ListN").stream()
                .anyMatch(importName::contains);
        if (!alreadyExists && !isImmutableCollection) {
            utCu.addImport(importName, isStatic, isAsterisk);
        }
    }

    private boolean matchesImport(final ImportDeclaration importDeclaration, final String importName, final Boolean isStatic, final Boolean isAsterisk) {
        final String nameAsString = importDeclaration.getNameAsString();
        final boolean aStatic = importDeclaration.isStatic();
        final boolean aAsterisk = importDeclaration.isAsterisk();
        final boolean matchesName = nameAsString.equals(importName);
        final boolean matchesStatic = isStatic == aStatic;
        final boolean matchesAsterisk = isAsterisk == aAsterisk;
        return matchesName && matchesStatic && matchesAsterisk;
    }

    public String addInlinedMock(final CompilationUnit utCu, final String simpleName) {
        this.addImportIfNotExists(utCu, "org.mockito.Mockito.mock", true, false);
        return MOCK_OBJECT_PATTERN.formatted(simpleName);
    }
}
