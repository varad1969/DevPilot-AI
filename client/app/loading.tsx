import { Spinner } from "@/components/ui/spinner";

export default function RootLoading() {
  return (
    <div className="flex min-h-svh items-center justify-center bg-background">
      <div className="flex flex-col items-center gap-3 text-muted-foreground">
        <Spinner className="size-8 text-primary" />
        <p className="text-sm font-medium animate-pulse">Loading DevPilot…</p>
      </div>
    </div>
  );
}
