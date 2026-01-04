package com.theatomicity.aop.ut.generator.core;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.Name;
import org.springframework.stereotype.Component;

@Component
public class GeneratorUtils {

    public ClassOrInterfaceDeclaration getMainType(final CompilationUnit originCompilationUnit) {
        return (ClassOrInterfaceDeclaration) originCompilationUnit.getType(0);
    }

    public String getInstanceName(final String originClassName) {
        return Character.toLowerCase(originClassName.charAt(0)) + originClassName.substring(1);
    }

    public String getOriginFullClassName(final CompilationUnit originCompilationUnit) {
        final ClassOrInterfaceDeclaration originClass = this.getMainType(originCompilationUnit);
        final String classNameAsString = originClass.getNameAsString();
        final String packageNameAsString = originCompilationUnit.getPackageDeclaration()
                .map(PackageDeclaration::getName)
                .map(Name::asString)
                .orElse("");
        return String.format("%s.%s", packageNameAsString, classNameAsString);
    }

}
