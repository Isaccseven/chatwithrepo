import { AstDocument } from './AstService';

export class DependencyService {
  public analyzeDependencies(documents: AstDocument[]): DependencyData {
    const nodes: Map<string, DependencyNode> = new Map();
    const dependencyMap: Map<string, Set<string>> = new Map();
    const classToFileMap: Map<string, string> = new Map();

    // First pass: Create nodes for all files and collect their dependencies
    for (const doc of documents) {
      try {
        const filePath = doc.filePath;
        const fileId = filePath;

        // Add file node
        nodes.set(fileId, new DependencyNode(
          fileId,
          this.getSimpleFileName(filePath),
          'file',
          this.calculateFileSize(doc.rawContent)
        ));

        // Store dependencies for this file
        dependencyMap.set(fileId, new Set());

        // Map package names to file paths for dependency resolution
        const packageName = doc.metadata.packageName;
        if (packageName) {
          for (const className of doc.metadata.classes) {
            const fullClassName = `${packageName}.${className}`;
            classToFileMap.set(fullClassName, fileId);
          }
        }
      } catch (error) {
        console.error(`Failed to process document: ${doc.filePath}`, error);
      }
    }

    // Second pass: Create dependency links between files
    const links: DependencyLink[] = [];

    for (const doc of documents) {
      try {
        const sourceFile = doc.filePath;

        // Process each import statement
        for (const dependency of doc.metadata.dependencies) {
          const targetFile = classToFileMap.get(dependency);
          if (targetFile && targetFile !== sourceFile) {
            dependencyMap.get(sourceFile)?.add(targetFile);
          }
        }
      } catch (error) {
        console.error(`Failed to process dependencies for: ${doc.filePath}`, error);
      }
    }

    // Convert dependency map to links
    dependencyMap.forEach((targets, source) => {
      targets.forEach(target => {
        links.push(new DependencyLink(source, target, 1));
      });
    });

    return new DependencyData(Array.from(nodes.values()), links);
  }

  private calculateFileSize(content: string): number {
    return Math.min(100, Math.max(20, content.length / 100));
  }

  private getSimpleFileName(filePath: string): string {
    const lastSlash = filePath.lastIndexOf('/');
    return lastSlash === -1 ? filePath : filePath.substring(lastSlash + 1);
  }

  private getSimpleName(fullyQualifiedName: string): string {
    if (!fullyQualifiedName) {
      return '';
    }
    const lastDot = fullyQualifiedName.lastIndexOf('.');
    return lastDot === -1 ? fullyQualifiedName : fullyQualifiedName.substring(lastDot + 1);
  }
}

export class DependencyNode {
  constructor(
    public id: string,
    public name: string,
    public type: 'file' | 'class' | 'interface' | 'method',
    public size: number
  ) {}
}

export class DependencyLink {
  constructor(
    public source: string,
    public target: string,
    public value: number
  ) {}
}

export class DependencyData {
  constructor(
    public nodes: DependencyNode[],
    public links: DependencyLink[]
  ) {}
}
