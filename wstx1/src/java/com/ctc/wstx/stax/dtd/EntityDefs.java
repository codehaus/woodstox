package com.ctc.wstx.stax.dtd;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ctc.wstx.stax.evt.WEntityDeclaration;

/**
 * Simple Map-like container that not only holds entities for quick access,
 * but also keeps track if any has been accessed, and optionally, if so,
 * which ones.
 */
public final class EntityDefs
{
  // // // Configuration

  private final int mMapSize;

  private boolean mTrackAccessed = false;


  // // // Entity data

  /**
   * Lazy-constructed Map that contains all entities defined.
   */
  private HashMap mEntities = null;


  // // // Access info

  private boolean mAnyAccessed = false;

  private HashSet mAccessed = null;

  /*
  ////////////////////////////////////////////////////
  // Life-cycle, configuration
  ////////////////////////////////////////////////////
   */

  public EntityDefs()
  {
    this(-1);
  }

  public EntityDefs(int size)
  {
    mMapSize = size;
  }

  public void setTrackAccessed(boolean state) {
    mTrackAccessed = state;
  }

  /*
  ////////////////////////////////////////////////////
  // Public API
  ////////////////////////////////////////////////////
   */

  public boolean anyAccessed() {
    return mAnyAccessed || (mAccessed != null && !mAccessed.isEmpty());
  }

  public WEntityDeclaration findEntity(String id)
  {
    if (mEntities == null) {
      return null;
    }
    WEntityDeclaration ed = (WEntityDeclaration) mEntities.get(id);
    if (ed != null) {
      mAnyAccessed = true;
      if (mTrackAccessed) {
        if (mAccessed == null) {
          mAccessed = new HashSet();
        }
        mAccessed.add(id);
      }
    }
    return ed;
  }

  public Map getEntities() {
    return (mEntities == null) ? Collections.EMPTY_MAP : mEntities;
  }

  public Set getAccessedEntityIds() {
    return mAccessed;
  }

  public void addEntity(String id, WEntityDeclaration ed)
  {
    if (mEntities == null) {
      mEntities = (mMapSize < 2) ? new HashMap() : new HashMap(mMapSize);
    }
    mEntities.put(id, ed);
  }

  /*
  ////////////////////////////////////////////////////
  // Internal methods
  ////////////////////////////////////////////////////
   */
}
