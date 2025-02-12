package io.github.thebusybiscuit.slimefun4.implementation.tasks.player;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import javax.annotation.Nonnull;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

abstract class AbstractPlayerTask implements Runnable {

    protected final Player p;
    private WrappedTask task;

    AbstractPlayerTask(@Nonnull Player p) {
        this.p = p;
    }

    private void setCancelTask(WrappedTask task){
        this.task = task;
    }

    public void schedule(long delay) {
        setCancelTask(Slimefun.getFoliaLib().getScheduler().runLater(this,delay));
    }

    public void scheduleRepeating(long delay, long interval) {
        setCancelTask(Slimefun.getFoliaLib().getScheduler().runTimer(this,delay,interval));
    }

    @Override
    public final void run() {
        if (isValid()) {
            executeTask();
        }
    }

    /**
     * This method cancels this {@link AbstractPlayerTask}.
     */
    public final void cancel() {
        Slimefun.getFoliaLib().getScheduler().cancelTask(task);
    }

    /**
     * This method checks if this {@link AbstractPlayerTask} should be continued or cancelled.
     * It will also cancel this {@link AbstractPlayerTask} if it became invalid.
     *
     * @return Whether this {@link AbstractPlayerTask} is still valid
     */
    protected boolean isValid() {
        if (!p.isOnline() || !p.isValid() || p.isDead() || !p.isSneaking()) {
            cancel();
            return false;
        }

        return true;
    }

    protected abstract void executeTask();
}
