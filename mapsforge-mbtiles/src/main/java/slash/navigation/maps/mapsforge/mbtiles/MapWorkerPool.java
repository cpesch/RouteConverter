package slash.navigation.maps.mapsforge.mbtiles;

import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.util.Parameters;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.queue.JobQueue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

class MapWorkerPool implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(MapWorkerPool.class.getName());

    public static boolean DEBUG_TIMING = false;

    private final AtomicInteger concurrentJobs = new AtomicInteger();
    private final AtomicLong totalExecutions = new AtomicLong();
    private final AtomicLong totalTime = new AtomicLong();

    private final DatabaseRenderer databaseRenderer;
    private boolean inShutdown, isRunning;
    private final JobQueue<RendererJob> jobQueue;
    private final Layer layer;
    private ExecutorService self, workers;
    private final TileCache tileCache;

    MapWorkerPool(TileCache tileCache, JobQueue<RendererJob> jobQueue, DatabaseRenderer databaseRenderer, TileMBTilesLayer layer) {
        super();
        this.tileCache = tileCache;
        this.jobQueue = jobQueue;
        this.databaseRenderer = databaseRenderer;
        this.layer = layer;
        this.inShutdown = false;
        this.isRunning = false;
    }

    public void run() {
        try {
            while (!inShutdown) {
                RendererJob rendererJob = this.jobQueue.get(Parameters.NUMBER_OF_THREADS);
                if (rendererJob == null) {
                    continue;
                }
                if (!this.tileCache.containsKey(rendererJob)) {
                    workers.execute(new MapWorker(rendererJob));
                } else {
                    jobQueue.remove(rendererJob);
                }
            }
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "MapWorkerPool interrupted", e);
        } catch (RejectedExecutionException e) {
            LOGGER.log(Level.SEVERE, "MapWorkerPool rejected", e);
        }
    }

    public synchronized void start() {
        if (this.isRunning) {
            return;
        }
        this.inShutdown = false;
        this.self = Executors.newSingleThreadExecutor();
        this.workers = Executors.newFixedThreadPool(Parameters.NUMBER_OF_THREADS);
        this.self.execute(this);
        this.isRunning = true;
    }

    public synchronized void stop() {
        if (!this.isRunning) {
            return;
        }
        this.inShutdown = true;
        this.jobQueue.interrupt();

        // Shutdown executors
        this.self.shutdown();
        this.workers.shutdown();

        try {
            if (!this.self.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                this.self.shutdownNow();
                if (!this.self.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                    LOGGER.fine("Shutdown self executor failed");
                }
            }
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Shutdown self executor interrupted", e);
        }

        try {
            if (!this.workers.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                this.workers.shutdownNow();
                if (!this.workers.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                    LOGGER.fine("Shutdown workers executor failed");
                }
            }
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Shutdown workers executor interrupted", e);
        }

        this.isRunning = false;
    }

    class MapWorker implements Runnable {
        private final RendererJob rendererJob;

        MapWorker(RendererJob rendererJob) {
            this.rendererJob = rendererJob;
        }

        public void run() {
            TileBitmap bitmap = null;
            try {
                long start = 0;
                if (inShutdown) {
                    return;
                }
                if (DEBUG_TIMING) {
                    start = System.currentTimeMillis();
                    LOGGER.info("ConcurrentJobs " + concurrentJobs.incrementAndGet());
                }
                bitmap = databaseRenderer.executeJob(rendererJob);
                if (inShutdown) {
                    return;
                }

                if (bitmap != null) {
                    tileCache.put(rendererJob, bitmap);
                }
                layer.requestRedraw();

                if (DEBUG_TIMING) {
                    long end = System.currentTimeMillis();
                    long te = totalExecutions.incrementAndGet();
                    long tt = totalTime.addAndGet(end - start);
                    if (te % 10 == 0) {
                        LOGGER.info("TIMING " + Long.toString(te) + " " + Double.toString(tt / te));
                    }
                    concurrentJobs.decrementAndGet();
                }
            } finally {
                jobQueue.remove(rendererJob);
                if (bitmap != null) {
                    bitmap.decrementRefCount();
                }
            }
        }
    }
}
