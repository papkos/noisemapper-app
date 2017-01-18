package no.uio.ifi.akosp.noisemapper.model;

import org.greenrobot.greendao.DaoException;
import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.ToOne;

import java.util.Date;

/**
 * Created on 2016.09.25..
 *
 * @author Ákos Pap
 */
@Entity
public class Record {
    @Id(autoincrement = true)
    protected Long id;

    @NotNull
    protected String uuid;

    @NotNull
    protected Date timestamp;

    @ToOne(joinProperty = "stateId")
    protected State state;
    protected Long stateId;

    protected String filename;

    protected boolean processed = false;

    /** Used to resolve relations */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;

    /** Used for active entity operations. */
    @Generated(hash = 765166123)
    private transient RecordDao myDao;

    @Generated(hash = 1230296149)
    public Record(Long id, @NotNull String uuid, @NotNull Date timestamp, Long stateId,
            String filename, boolean processed) {
        this.id = id;
        this.uuid = uuid;
        this.timestamp = timestamp;
        this.stateId = stateId;
        this.filename = filename;
        this.processed = processed;
    }

    @Generated(hash = 477726293)
    public Record() {
    }

    @Generated(hash = 1617069713)
    private transient Long state__resolvedKey;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Date getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getFilename() {
        return this.filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public boolean getProcessed() {
        return this.processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public Long getStateId() {
        return this.stateId;
    }

    public void setStateId(Long stateId) {
        this.stateId = stateId;
    }

    /** To-one relationship, resolved on first access. */
    @Generated(hash = 935789366)
    public State getState() {
        Long __key = this.stateId;
        if (state__resolvedKey == null || !state__resolvedKey.equals(__key)) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            StateDao targetDao = daoSession.getStateDao();
            State stateNew = targetDao.load(__key);
            synchronized (this) {
                state = stateNew;
                state__resolvedKey = __key;
            }
        }
        return state;
    }

    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 597277186)
    public void setState(State state) {
        synchronized (this) {
            this.state = state;
            stateId = state == null ? null : state.getId();
            state__resolvedKey = stateId;
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
    @Generated(hash = 1505145191)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getRecordDao() : null;
    }

}
