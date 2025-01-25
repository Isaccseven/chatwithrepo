package com.codeium.chatcodebase.service;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class AstService {
    private static final Set<String> JAVA_EXTENSIONS = Set.of(".java", ".kt", ".scala");

    public List<AstDocument> parseFiles(List<GitLabService.CodeFile> files) {
        return files.parallelStream()
            .filter(file -> isJavaFile(file.path()))
            .map(this::parseFile)
            .toList();
    }

    private boolean isJavaFile(String path) {
        return JAVA_EXTENSIONS.stream()
            .anyMatch(path::endsWith);
    }

    private AstDocument parseFile(GitLabService.CodeFile file) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(file.content());
            AstMetadata metadata = extractMetadata(cu);
            
            return new AstDocument(
                file.path(),
                file.content(),
                serializeAst(cu),
                metadata,
                file.lastCommitId()
            );
        } catch (ParseProblemException e) {
            throw new AstParseException("Failed to parse file: " + file.path(), e);
        }
    }

    private AstMetadata extractMetadata(CompilationUnit cu) {
        AstMetadata metadata = new AstMetadata();
        
        // Class/Interface declarations
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
            metadata.addClass(cls.getNameAsString());
            cls.getMethods().forEach(method -> 
                metadata.addMethod(method.getDeclarationAsString()));
            cls.getFields().forEach(field -> 
                metadata.addField(field.getVariables().get(0).getNameAsString()));
        });

        // Import statements
        cu.getImports().forEach(importDecl -> 
            metadata.addDependency(importDecl.getNameAsString()));

        // Package info
        cu.getPackageDeclaration().ifPresent(pkg -> 
            metadata.setPackageName(pkg.getNameAsString()));

        return metadata;
    }

    private String serializeAst(CompilationUnit cu) {
        return cu.toString();
    }

    @Getter
    public static class AstDocument {
        private final String filePath;
        private final String rawContent;
        private final String astContent;
        private final AstMetadata metadata;
        private final String lastCommitId;

        public AstDocument(String filePath, String rawContent, String astContent, 
                         AstMetadata metadata, String lastCommitId) {
            this.filePath = filePath;
            this.rawContent = rawContent;
            this.astContent = astContent;
            this.metadata = metadata;
            this.lastCommitId = lastCommitId;
        }
    }

    @Getter
    public static class AstMetadata {
        private String packageName;
        private final List<String> classes = new ArrayList<>();
        private final List<String> methods = new ArrayList<>();
        private final List<String> fields = new ArrayList<>();
        private final List<String> dependencies = new ArrayList<>();

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        public void addClass(String className) {
            classes.add(className);
        }

        public void addMethod(String methodDeclaration) {
            methods.add(methodDeclaration);
        }

        public void addField(String fieldName) {
            fields.add(fieldName);
        }

        public void addDependency(String dependency) {
            dependencies.add(dependency);
        }
    }

    public static class AstParseException extends RuntimeException {
        public AstParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
