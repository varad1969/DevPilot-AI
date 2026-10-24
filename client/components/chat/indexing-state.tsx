"use client";

import { AlertCircle, BookOpen, Loader2, RotateCcw, Sparkles } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Empty,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@/components/ui/empty";
import { Progress } from "@/components/ui/progress";
import { getRepoProgress, useStartIndexing } from "@/hooks/use-repos";
import type { IndexStatusResponse, Repository } from "@/lib/api";

export function IndexingState({
  repo,
  status,
}: {
  repo: Repository;
  status?: IndexStatusResponse;
}) {
  const indexMutation = useStartIndexing();
  const filesProcessed = status?.filesProcessed ?? repo.filesProcessed;
  const filesTotal = status?.filesTotal ?? repo.filesTotal;
  const chunkCount = status?.chunkCount ?? repo.chunkCount;
  const progress = getRepoProgress({ filesProcessed, filesTotal });
  const indexStatus = status?.indexStatus ?? repo.indexStatus;
  const errorMessage = status?.errorMessage ?? repo.errorMessage;

  if (indexStatus === "FAILED") {
    return (
      <Empty className="h-full border-0">
        <EmptyHeader>
          <EmptyMedia variant="icon">
            <AlertCircle className="text-destructive" />
          </EmptyMedia>
          <EmptyTitle>Indexing failed</EmptyTitle>
          <EmptyDescription>
            {errorMessage || "Something went wrong while indexing this repository."}
          </EmptyDescription>
        </EmptyHeader>
        <Button
          onClick={() => indexMutation.mutate(repo.id)}
          disabled={indexMutation.isPending}
        >
          {indexMutation.isPending ? (
            <>
              <Loader2 className="animate-spin" data-icon="inline-start" />
              Starting indexing…
            </>
          ) : (
            <>
              <RotateCcw data-icon="inline-start" />
              Retry indexing
            </>
          )}
        </Button>
      </Empty>
    );
  }

  if (indexStatus === "PENDING" && !indexMutation.isPending) {
    return (
      <Empty className="h-full border-0">
        <EmptyHeader>
          <EmptyMedia variant="icon">
            <BookOpen className="text-primary" />
          </EmptyMedia>
          <EmptyTitle>Repository not indexed yet</EmptyTitle>
          <EmptyDescription>
            Index this repository to extract code chunks, build semantic embeddings, and unlock AI chat.
          </EmptyDescription>
        </EmptyHeader>
        <Button
          onClick={() => indexMutation.mutate(repo.id)}
          disabled={indexMutation.isPending}
          size="lg"
        >
          <Sparkles data-icon="inline-start" />
          Start indexing
        </Button>
      </Empty>
    );
  }

  if (indexStatus === "EXPIRED" && !indexMutation.isPending) {
    return (
      <Empty className="h-full border-0">
        <EmptyHeader>
          <EmptyMedia variant="icon">
            <RotateCcw className="text-amber-500" />
          </EmptyMedia>
          <EmptyTitle>Index expired</EmptyTitle>
          <EmptyDescription>
            The indexed code chunks have expired. Re-index to resume chatting.
          </EmptyDescription>
        </EmptyHeader>
        <Button
          onClick={() => indexMutation.mutate(repo.id)}
          disabled={indexMutation.isPending}
          size="lg"
        >
          <RotateCcw data-icon="inline-start" />
          Re-index repository
        </Button>
      </Empty>
    );
  }

  return (
    <Empty className="h-full border-0">
      <EmptyHeader>
        <EmptyMedia variant="icon">
          <Loader2 className="animate-spin" />
        </EmptyMedia>
        <EmptyTitle>Indexing {repo.fullName}</EmptyTitle>
        <EmptyDescription>
          {filesTotal > 0
            ? `${filesProcessed} of ${filesTotal} files · ${chunkCount} chunks embedded`
            : "Fetching repository files and preparing embeddings…"}
        </EmptyDescription>
      </EmptyHeader>
      <div className="w-full max-w-sm space-y-2">
        <Progress value={Math.max(progress, filesTotal ? progress : 12)} />
        <p className="text-center text-xs text-muted-foreground">
          You can leave this page open — chat unlocks when indexing finishes.
        </p>
      </div>
    </Empty>
  );
}
