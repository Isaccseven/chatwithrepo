import { Document } from 'langchain/document';
import { VectorStore } from 'langchain/vectorstores';
import { AstDocument } from './AstService';
import { EmbeddingService } from './EmbeddingService';
import { MetadataService } from './MetadataService';

export class VectorStoreService {
  private vectorStore: VectorStore;
  private embeddingService: EmbeddingService;
  private metadataService: MetadataService;
  private chunkSize: number;
  private chunkOverlap: number;

  constructor(vectorStore: VectorStore, embeddingService: EmbeddingService, metadataService: MetadataService, chunkSize: number = 6000, chunkOverlap: number = 500) {
    this.vectorStore = vectorStore;
    this.embeddingService = embeddingService;
    this.metadataService = metadataService;
    this.chunkSize = chunkSize;
    this.chunkOverlap = chunkOverlap;
  }

  public async storeAstDocuments(documents: AstDocument[]): Promise<void> {
    for (const doc of documents) {
      try {
        const chunks = this.embeddingService.generateEmbeddings(doc);
        if (chunks.length > 0) {
          await this.vectorStore.add(chunks);
        }
      } catch (error) {
        console.error(`Failed to process document: ${doc.filePath}`, error);
      }
    }
  }

  public async semanticSearch(query: string): Promise<Document[]> {
    const foundDocuments = await this.vectorStore.similaritySearch({
      query,
      topK: 10,
      similarityThreshold: 0.8
    });
    console.info(`Found ${foundDocuments.length} relevant documents for query: ${query}`);
    return foundDocuments;
  }
}
