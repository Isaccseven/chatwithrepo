import { ChatOpenAI } from 'langchain/chat_models/openai';
import { HumanChatMessage, AIChatMessage } from 'langchain/schema';
import { VectorStoreService } from './VectorStoreService';

export class ChatService {
  private chatModel: ChatOpenAI;
  private vectorStore: VectorStoreService;

  constructor(apiKey: string, vectorStore: VectorStoreService) {
    this.chatModel = new ChatOpenAI({
      openAIApiKey: apiKey,
      temperature: 0.7,
      modelName: 'gpt-3.5-turbo',
    });
    this.vectorStore = vectorStore;
  }

  public async chatWithContext(query: string): Promise<string> {
    const systemPrompt = `
      You are a senior Java developer assistant analyzing a codebase.
      Use the following code context to answer the user's question.

      You can use the getFileDependencies function to get dependencies for specific files.
      This function takes a filePath parameter and returns:
      - fileName: The name of the file
      - dependencies: A list of files, classes or methods that this file depends on

      {context}

      When referencing code, use specific file names, class names, and line numbers.
      Format code examples in markdown with appropriate language tags.
      Keep responses concise but informative, focusing on the most relevant parts of the codebase.
    `;

    const transformedQuery = await this.transformQuery(query);
    const context = await this.getContext(transformedQuery);

    const messages = [
      new HumanChatMessage(systemPrompt.replace('{context}', context)),
      new HumanChatMessage(transformedQuery),
    ];

    const response = await this.chatModel.call(messages);
    return response.text;
  }

  private async transformQuery(query: string): Promise<string> {
    // Implement query transformation logic here
    return query;
  }

  private async getContext(query: string): Promise<string> {
    const documents = await this.vectorStore.semanticSearch(query);
    return documents.map(doc => doc.content).join('\n');
  }
}
