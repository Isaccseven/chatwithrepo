import { Document } from 'langchain/document';
import { VectorStoreService } from './VectorStoreService';
import { AstService } from './AstService';

export class EmbeddingService {
  private vectorStoreService: VectorStoreService;

  constructor(vectorStoreService: VectorStoreService) {
    this.vectorStoreService = vectorStoreService;
  }

  public generateEmbeddings(doc: AstService.AstDocument): Document[] {
    const content = doc.rawContent;
    if (!content) {
      return [];
    }

    const chunkSize = this.vectorStoreService.chunkSize;
    const chunkOverlap = this.vectorStoreService.chunkOverlap;

    if (content.length / 4 <= chunkSize) {
      return [this.convertToDocument(doc, content, 1, 1)];
    }

    const documents: Document[] = [];
    let currentChunk = '';
    let chunkIndex = 1;
    const totalChunks = this.estimateTotalChunks(content.length, chunkSize);

    for (let i = 0; i < content.length; i += chunkSize * 4) {
      currentChunk += content.slice(i, i + chunkSize * 4);

      while (currentChunk.length >= chunkSize * 4) {
        const endIndex = this.findChunkEndIndex(currentChunk, chunkSize, chunkOverlap);
        documents.push(this.convertToDocument(
          doc,
          currentChunk.slice(0, endIndex),
          chunkIndex++,
          totalChunks
        ));

        if (endIndex < currentChunk.length) {
          currentChunk = currentChunk.slice(Math.max(0, endIndex - chunkOverlap * 4));
        } else {
          currentChunk = '';
        }
      }
    }

    if (currentChunk.length > 0) {
      documents.push(this.convertToDocument(
        doc,
        currentChunk,
        chunkIndex,
        totalChunks
      ));
    }

    return documents;
  }

  private estimateTotalChunks(contentLength: number, chunkSize: number): number {
    const chunkSizeInChars = chunkSize * 4;
    return Math.ceil(contentLength / chunkSizeInChars);
  }

  private findChunkEndIndex(content: string, chunkSize: number, chunkOverlap: number): number {
    const targetEnd = Math.min(content.length, chunkSize * 4);
    const searchStart = Math.max(0, targetEnd - chunkOverlap * 4);

    for (const boundary of ["\n}", ";\n", "\n\n", "\n", ". "]) {
      const index = content.lastIndexOf(boundary, targetEnd);
      if (index >= searchStart) {
        return index + boundary.length;
      }
    }

    return targetEnd;
  }

  private convertToDocument(doc: AstService.AstDocument, chunkContent: string, chunkIndex: number, totalChunks: number): Document {
    const metadata = this.vectorStoreService.enhanceMetadata(doc, chunkIndex, totalChunks);

    return new Document({
      content: `File: ${doc.filePath} (${chunkIndex}/${totalChunks})\n` +
               `Package: ${doc.metadata.packageName}\n` +
               `Classes: ${doc.metadata.classes.join(', ')}\n` +
               `Methods: ${doc.metadata.methods.join(', ')}\n` +
               `Content:\n${chunkContent}\n`,
      metadata
    });
  }
}
