import { Skeleton } from "@/components/ui/skeleton";

export default function ChatLoading() {
  return (
    <div className="flex min-h-svh flex-col bg-background">
      <div className="flex h-14 items-center justify-between border-b px-6">
        <div className="flex items-center gap-3">
          <Skeleton className="h-6 w-40 rounded-md" />
          <Skeleton className="h-5 w-16 rounded-full" />
        </div>
        <Skeleton className="h-8 w-20 rounded-md" />
      </div>

      <div className="flex flex-1 flex-col md:flex-row">
        {/* Sidebar skeleton */}
        <div className="w-full border-b p-4 md:w-72 md:border-r md:border-b-0 space-y-4">
          <Skeleton className="h-9 w-full rounded-lg" />
          <div className="space-y-2">
            <Skeleton className="h-8 w-full rounded-md" />
            <Skeleton className="h-8 w-full rounded-md" />
            <Skeleton className="h-8 w-full rounded-md" />
          </div>
        </div>

        {/* Chat area skeleton */}
        <div className="flex flex-1 flex-col justify-between p-6 space-y-6">
          <div className="space-y-4">
            <div className="flex gap-3">
              <Skeleton className="size-8 rounded-full" />
              <Skeleton className="h-16 w-3/4 rounded-2xl" />
            </div>
            <div className="flex justify-end gap-3">
              <Skeleton className="h-12 w-1/2 rounded-2xl" />
            </div>
          </div>
          <Skeleton className="h-14 w-full rounded-xl" />
        </div>
      </div>
    </div>
  );
}
