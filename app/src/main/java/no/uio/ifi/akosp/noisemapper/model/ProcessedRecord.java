package no.uio.ifi.akosp.noisemapper.model;

import org.greenrobot.greendao.DaoException;
import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.ToOne;

import java.util.Date;

/**
 * Created on 2016.10.07..
 *
 * @author Ákos Pap
 */
@Entity
public class ProcessedRecord {

    @Id(autoincrement = true)
    protected Long id;

    @NotNull
    protected Date timestamp;

    @ToOne
    protected State state;

    protected boolean uploaded = false;

    protected String processResult;

    /** Used to resolve relations */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;

    /** Used for active entity operations. */
    @Generated(hash = 1576352220)
    private transient ProcessedRecordDao myDao;

    @Generated(hash = 495264241)
    public ProcessedRecord(Long id, @NotNull Date timestamp, boolean uploaded,
            String processResult) {
        this.id = id;
        this.timestamp = timestamp;
        this.uploaded = uploaded;
        this.processResult = processResult;
    }

    @Generated(hash = 1288854789)
    public ProcessedRecord() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public boolean getUploaded() {
        return this.uploaded;
    }

    public void setUploaded(boolean uploaded) {
        this.uploaded = uploaded;
    }

    public String getProcessResult() {
        return this.processResult;
    }

    public void setProcessResult(String processResult) {
        this.processResult = processResult;
    }

    @Generated(hash = 2121903627)
    private transient boolean state__refreshed;

    /** To-one relationship, resolved on first access. */
    @Generated(hash = 1228378983)
    public State getState() {
        if (state != null || !state__refreshed) {
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            StateDao targetDao = daoSession.getStateDao();
            targetDao.refresh(state);
            state__refreshed = true;
        }
        return state;
    }

    /** To-one relationship, returned entity is not refreshed and may carry only the PK property. */
    @Generated(hash = 1715569450)
    public State peakState() {
        return state;
    }

    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 646489265)
    public void setState(State state) {
        synchronized (this) {
            this.state = state;
            state__refreshed = true;
        }
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#delete(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 128553479)
    public void delete() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.delete(this);
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#refresh(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 1942392019)
    public void refresh() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.refresh(this);
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#update(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 713229351)
    public void update() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.update(this);
    }

    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 1877917022)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getProcessedRecordDao() : null;
    }
}
