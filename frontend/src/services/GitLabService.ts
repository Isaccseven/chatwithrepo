import axios from 'axios';

export class GitLabService {
  private baseUrl: string;
  private token: string;
  private defaultBranch: string;

  constructor(baseUrl: string, token: string, defaultBranch: string = 'main') {
    this.baseUrl = baseUrl;
    this.token = token;
    this.defaultBranch = defaultBranch;
  }

  private async getProject(projectIdOrPath: string) {
    const response = await axios.get(`${this.baseUrl}/projects/${encodeURIComponent(projectIdOrPath)}`, {
      headers: { 'PRIVATE-TOKEN': this.token },
    });
    return response.data;
  }

  private async getRepositoryTree(projectId: string) {
    const response = await axios.get(`${this.baseUrl}/projects/${projectId}/repository/tree`, {
      headers: { 'PRIVATE-TOKEN': this.token },
      params: { ref: this.defaultBranch, recursive: true },
    });
    return response.data;
  }

  private async getFileContent(projectId: string, filePath: string) {
    const response = await axios.get(`${this.baseUrl}/projects/${projectId}/repository/files/${encodeURIComponent(filePath)}/raw`, {
      headers: { 'PRIVATE-TOKEN': this.token },
      params: { ref: this.defaultBranch },
    });
    return response.data;
  }

  public async fetchRepository(projectIdOrPath: string) {
    await this.getProject(projectIdOrPath);
    const tree = await this.getRepositoryTree(projectIdOrPath);
    return this.processTreeNodes(projectIdOrPath, tree);
  }

  private async processTreeNodes(projectId: string, nodes: any[]) {
    const supportedExtensions = ['.java', '.kt', '.py', '.js', '.ts', '.jsx', '.tsx', '.xml', '.yml', '.yaml', '.json', '.md', '.txt'];
    const maxFileSizeBytes = 100000;

    const files = await Promise.all(
      nodes
        .filter(node => node.type === 'blob' && supportedExtensions.some(ext => node.path.endsWith(ext)))
        .map(async node => {
          const content = await this.getFileContent(projectId, node.path);
          if (content.length > maxFileSizeBytes) {
            return null;
          }
          return { path: node.path, content, mode: node.mode };
        })
    );

    return files.filter(file => file !== null);
  }
}
