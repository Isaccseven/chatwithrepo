import { GitLabService } from './GitLabService';
import { AstService, AstDocument } from './AstService';
import { VectorStoreService } from './VectorStoreService';
import { DependencyService, DependencyData } from './DependencyService';
import { AnalysisStatus, AnalysisStep } from './types';

export class CodeAnalysisOrchestrator {
  private gitLabService: GitLabService;
  private astService: AstService;
  private vectorStoreService: VectorStoreService;
  private dependencyService: DependencyService;
  private analysisStatusMap: Map<string, AnalysisStatus>;
  private dependencyDataMap: Map<string, DependencyData>;

  constructor(
    gitLabService: GitLabService,
    astService: AstService,
    vectorStoreService: VectorStoreService,
    dependencyService: DependencyService
  ) {
    this.gitLabService = gitLabService;
    this.astService = astService;
    this.vectorStoreService = vectorStoreService;
    this.dependencyService = dependencyService;
    this.analysisStatusMap = new Map();
    this.dependencyDataMap = new Map();
  }

  public async analyzeRepository(projectId: string): Promise<void> {
    const status = new AnalysisStatus();
    this.analysisStatusMap.set(projectId, status);

    try {
      // Step 1: Fetch repository files
      status.setCurrentStep(AnalysisStep.FETCHING_FILES);
      const files = await this.gitLabService.fetchRepository(projectId);
      status.setProgress(20);

      // Step 2: Parse AST
      status.setCurrentStep(AnalysisStep.PARSING_AST);
      const astDocs = this.astService.parseFiles(files);
      status.setProgress(40);

      // Step 3: Analyze dependencies
      status.setCurrentStep(AnalysisStep.ANALYZING_DEPENDENCIES);
      const dependencyData = this.dependencyService.analyzeDependencies(astDocs);
      this.dependencyDataMap.set(projectId, dependencyData);
      status.setProgress(60);

      // Step 4: Generate embeddings and store
      status.setCurrentStep(AnalysisStep.STORING_VECTORS);
      this.vectorStoreService.storeAstDocuments(astDocs);
      status.setProgress(100);

      status.setCurrentStep(AnalysisStep.COMPLETED);
      status.setSuccess(true);
    } catch (error) {
      status.setError((error as Error).message);
      status.setSuccess(false);
    }
  }

  public getAnalysisStatus(projectId: string): AnalysisStatus {
    return this.analysisStatusMap.get(projectId) || new AnalysisStatus(AnalysisStep.NOT_STARTED, 0, null, false);
  }

  public getDependencyData(projectId: string): DependencyData | undefined {
    return this.dependencyDataMap.get(projectId);
  }
}
