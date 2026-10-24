import { Skeleton } from "@/components/ui/skeleton";

export default function DashboardLoading() {
  return (
    <div className="flex min-h-svh flex-col bg-background">
      {/* Top bar placeholder */}
      <div className="flex h-14 items-center justify-between border-b px-6">
        <Skeleton className="h-6 w-36 rounded-md" />
        <div className="flex items-center gap-3">
          <Skeleton className="size-8 rounded-full" />
          <Skeleton className="size-8 rounded-lg" />
        </div>
      </div>

      {/* Main dashboard content placeholder */}
      <div className="flex flex-1 flex-col gap-6 p-6">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <Skeleton className="h-8 w-48 rounded-lg" />
            <Skeleton className="h-4 w-72 rounded-md" />
          </div>
          <div className="flex gap-2">
            <Skeleton className="h-9 w-28 rounded-lg" />
            <Skeleton className="h-9 w-24 rounded-lg" />
          </div>
        </div>

        {/* Stats Grid */}
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-28 rounded-2xl" />
          ))}
        </div>

        {/* Repositories Grid */}
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-56 rounded-2xl" />
          ))}
        </div>
      </div>
    </div>
  );
}
