import { parse } from '@babel/parser';
import traverse from '@babel/traverse';
import * as t from '@babel/types';

export class AstService {
  private static readonly SUPPORTED_EXTENSIONS = ['.java', '.kt', '.py', '.js', '.ts', '.jsx', '.tsx', '.xml', '.yml', '.yaml', '.json', '.md', '.txt'];

  public parseFiles(files: CodeFile[]): AstDocument[] {
    return files
      .filter(file => this.isSupportedFile(file.path))
      .map(file => this.parseFile(file));
  }

  private isSupportedFile(path: string): boolean {
    return AstService.SUPPORTED_EXTENSIONS.some(ext => path.endsWith(ext));
  }

  private parseFile(file: CodeFile): AstDocument {
    try {
      const ast = parse(file.content, {
        sourceType: 'module',
        plugins: ['typescript', 'jsx']
      });
      const metadata = this.extractMetadata(ast);
      return new AstDocument(file.path, file.content, JSON.stringify(ast, null, 2), metadata);
    } catch (error) {
      throw new AstParseException(`Failed to parse file: ${file.path}`, error);
    }
  }

  private extractMetadata(ast: t.File): AstMetadata {
    const metadata = new AstMetadata();

    traverse(ast, {
      enter(path) {
        if (t.isClassDeclaration(path.node) || t.isClassExpression(path.node)) {
          metadata.addClass(path.node.id?.name || 'AnonymousClass');
        } else if (t.isFunctionDeclaration(path.node) || t.isFunctionExpression(path.node)) {
          metadata.addMethod(path.node.id?.name || 'AnonymousFunction');
        } else if (t.isVariableDeclaration(path.node)) {
          path.node.declarations.forEach(decl => {
            if (t.isIdentifier(decl.id)) {
              metadata.addField(decl.id.name);
            }
          });
        } else if (t.isImportDeclaration(path.node)) {
          metadata.addDependency(path.node.source.value);
        } else if (t.isExportNamedDeclaration(path.node) && t.isStringLiteral(path.node.source)) {
          metadata.addDependency(path.node.source.value);
        }
      }
    });

    return metadata;
  }
}

export class AstDocument {
  constructor(
    public readonly filePath: string,
    public readonly rawContent: string,
    public readonly astContent: string,
    public readonly metadata: AstMetadata
  ) {}
}

export class AstMetadata {
  public packageName: string = '';
  public readonly classes: string[] = [];
  public readonly methods: string[] = [];
  public readonly fields: string[] = [];
  public readonly dependencies: string[] = [];

  public setPackageName(packageName: string): void {
    this.packageName = packageName;
  }

  public addClass(className: string): void {
    this.classes.push(className);
  }

  public addMethod(methodName: string): void {
    this.methods.push(methodName);
  }

  public addField(fieldName: string): void {
    this.fields.push(fieldName);
  }

  public addDependency(dependency: string): void {
    this.dependencies.push(dependency);
  }
}

export class AstParseException extends Error {
  constructor(message: string, public readonly cause: Error) {
    super(message);
  }
}

export interface CodeFile {
  path: string;
  content: string;
}
