/*
 * 
 * metadataTree.remove(smb);metadataTree.remove(smb); * CDDL HEADER START
 * 
 * The contents of this file are subject to the terms of the Common Development and Distribution
 * License, Version 1.0 only (the "License"). You may not use this file except in compliance with
 * the License.
 * 
 * You can obtain a copy of the license at license/ESCIDOC.LICENSE or http://www.escidoc.de/license.
 * See the License for the specific language governing permissions and limitations under the
 * License.
 * 
 * When distributing Covered Code, include this CDDL HEADER in each file and include the License
 * file at license/ESCIDOC.LICENSE. If applicable, add the following below this CDDL HEADER, with
 * the fields enclosed by brackets "[]" replaced with your own identifying information: Portions
 * Copyright [yyyy] [name of copyright owner]
 * 
 * CDDL HEADER END
 */
/*
 * Copyright 2006-2007 Fachinformationszentrum Karlsruhe Gesellschaft für
 * wissenschaftlich-technische Information mbH and Max-Planck- Gesellschaft zur Förderung der
 * Wissenschaft e.V. All rights reserved. Use is subject to license terms.
 */
package de.mpg.imeji.presentation.metadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.mpg.imeji.logic.vo.Metadata;

/**
 * Implements Hierarchy for a {@link List} of {@link SuperMetadataBean}
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SuperMetadataTree implements Serializable {
  private static final long serialVersionUID = 8726381279225329881L;
  private Map<String, SuperMetadataBean> map = new HashMap<String, SuperMetadataBean>();

  /**
   * Default Constructor
   */
  public SuperMetadataTree(List<SuperMetadataBean> list) {
    // Sort according to profile and to previous metadata position
    Collections.sort(list);
    // A Set the parent according to the current order
    list = setParents(list);
    // Create the map
    map = createMap(list, null, "0");
    // Set the childs
    for (SuperMetadataBean smb : map.values()) {
      smb.setChilds(getChilds(smb.getTreeIndex()));
    }
  }

  /**
   * Create the {@link Map} from the {@link List}
   * 
   * @param list
   * @param parent
   * @param index
   * @return
   */
  private Map<String, SuperMetadataBean> createMap(List<SuperMetadataBean> list,
      SuperMetadataBean parent, String index) {
    Map<String, SuperMetadataBean> map = new HashMap<String, SuperMetadataBean>();
    for (SuperMetadataBean smd : list) {
      if ((parent == null && isRootMetadata(smd)) || isChild(smd, parent)) {
        smd.setTreeIndex(index);
        map.put(smd.getTreeIndex(), smd);
        map.putAll(createMap(removeFromList(list, smd), smd, addIndex(index, "0")));
        index = incrementIndex(index);
      }
    }
    return map;
  }

  /**
   * True if the {@link SuperMetadataBean} is a root metadata
   * 
   * @param smb
   * @return
   */
  private boolean isRootMetadata(SuperMetadataBean smb) {
    return smb.getParent() == null;
  }

  /**
   * True if the {@link SuperMetadataBean} smb is child of parent
   * 
   * @param smb
   * @param parent
   * @return
   */
  private boolean isChild(SuperMetadataBean smb, SuperMetadataBean parent) {
    if (parent == null || smb.getParent() == null) {
      return false;
    } else {
      return smb.getParent().getMetadata().getId().compareTo(parent.getMetadata().getId()) == 0;
    }
  }

  /**
   * Remove the {@link SuperMetadataBean} smb from the {@link List}. A new list is returned, which
   * allows to use this method within the for loop on the list elements
   * 
   * @param list
   * @param smb
   * @return
   */
  private List<SuperMetadataBean> removeFromList(List<SuperMetadataBean> list,
      SuperMetadataBean smb) {
    List<SuperMetadataBean> subList = new ArrayList<SuperMetadataBean>(list);
    subList.remove(smb);
    return subList;
  }

  /**
   * Get the {@link SuperMetadataTree} as a {@link List}
   * 
   * @return
   */
  public List<SuperMetadataBean> getList() {
    List<SuperMetadataBean> list = new ArrayList<SuperMetadataBean>(map.values());
    Collections.sort(list, new TreeComparator());
    return resetPosition(list);
  }

  /**
   * Add a {@link SuperMetadataBean} to the {@link SuperMetadataTree} at the next position
   * 
   * @param smd
   */
  public void add(SuperMetadataBean smd) {
    String newIndex = incrementIndex(smd.getTreeIndex());
    smd.setTreeIndex(newIndex);
    insert(smd);
  }

  /**
   * Remove a {@link SuperMetadataBean} from the {@link SuperMetadataTree}. The childs of the
   * metadata are removed as well
   * 
   * @param smd
   */
  public void remove(SuperMetadataBean smd) {
    map.remove(smd.getTreeIndex());
    for (SuperMetadataBean child : getChilds(smd.getTreeIndex())) {
      map.remove(child.getTreeIndex());
    }
  }

  /**
   * Remove this metadata but don't remove its childs.
   * 
   * @param smd
   */
  public void removeButKeepChilds(SuperMetadataBean smd) {
    map.remove(smd.getTreeIndex());
  }

  /**
   * Insert a {@link SuperMetadataBean} and all its childs in the {@link SuperMetadataTree} at the
   * position defined in the getIndexTree() method. If the position is alreay used by a
   * {@link SuperMetadataBean}, move it to next position
   * 
   * @param smd
   */
  private void insert(SuperMetadataBean smd) {
    String insert = smd.getTreeIndex();
    if (map.containsKey(insert)) {
      moveToNextPosition(map.get(insert));
    }
    map.put(insert, smd);
    String childIndex = addIndex(smd.getTreeIndex(), "0");
    for (SuperMetadataBean child : smd.getChilds()) {
      child.setTreeIndex(childIndex);
      insert(child);
      childIndex = incrementIndex(childIndex);
    }
  }

  /**
   * Move a {@link SuperMetadataBean} and all its childs to the next position in the tree
   * 
   * @param smd
   */
  private void moveToNextPosition(SuperMetadataBean smd) {
    smd.setChilds(getChilds(smd.getTreeIndex()));
    remove(smd);
    smd.setTreeIndex(incrementIndex(smd.getTreeIndex()));
    insert(smd);
  }

  /**
   * Get the childs of tree element
   * 
   * @param parentIndex
   * @return
   */
  public List<SuperMetadataBean> getChilds(String parentIndex) {
    List<SuperMetadataBean> childs = new ArrayList<SuperMetadataBean>();
    // for (SuperMetadataBean smb : getList())
    for (SuperMetadataBean smb : map.values()) {
      if (isParent(parentIndex, smb.getTreeIndex())) {
        childs.add(smb);
      }
    }
    return childs;
  }

  /**
   * True if the index1 is a parent of the index2 (not necessary the direct parent):<br/>
   * - 1 is parent of all index 1,....
   * 
   * @param index1
   * @param index2
   * @return
   */
  private boolean isParent(String index1, String index2) {
    return index2.startsWith(index1 + ",") && index1.length() < index2.length();
  }

  /**
   * Increment the index to one position (1,1 -> 1,2 or 2,3,0,3 -> 2,3,0,4)
   * 
   * @param index
   * @return
   */
  private String incrementIndex(String index) {
    String[] indexes = index.split(",");
    int i = Integer.parseInt(indexes[indexes.length - 1]) + 1;
    int endIndex = index.lastIndexOf(",");
    if (endIndex > 0) {
      return addIndex(index.substring(0, endIndex), Integer.toString(i));
    }
    return Integer.toString(i);
  }

  /**
   * Add 2 indexes (1,2 + 1 = 1,2,1)
   * 
   * @param index1
   * @param index2
   * @return
   */
  private String addIndex(String index1, String index2) {
    return index1 = "".equals(index1) ? index2 : index1 + "," + index2;
  }

  /**
   * Reset the position to all Metadata according to the current order. The list should be flat
   */
  public static List<SuperMetadataBean> resetPosition(List<SuperMetadataBean> l) {
    int i = 0;
    for (SuperMetadataBean smd : l) {
      smd.setPos(i);
      i++;
    }
    return l;
  }

  /**
   * {@link Comparator} to sort the {@link SuperMetadataBean} in the {@link SuperMetadataTree}
   * 
   * @author saquet (initial creation)
   * @author $Author$ (last modification)
   * @version $Revision$ $LastChangedDate$
   */
  private class TreeComparator implements Comparator<SuperMetadataBean> {
    /*
     * (non-Javadoc)
     * 
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare(SuperMetadataBean md1, SuperMetadataBean md2) {
      String[] index1 = md1.getTreeIndex().split(",");
      String[] index2 = md2.getTreeIndex().split(",");
      int minLength = (index1.length < index2.length ? index1.length : index2.length);
      for (int i = 0; i < minLength; i++) {
        int v1 = Integer.parseInt(index1[i]);
        int v2 = Integer.parseInt(index2[i]);
        if (v1 > v2) {
          return 1;
        } else if (v1 < v2) {
          return -1;
        }
      }
      if (index1.length > index2.length) {
        return 1;
      } else if (index1.length < index2.length) {
        return -1;
      }
      return 0;
    }
  }

  /**
   * Set the Parent {@link SuperMetadataBean} to all element of the list. This is made according to
   * the current order of the list (see findBestParent() method)
   * 
   * @param flat
   * @return
   */
  private List<SuperMetadataBean> setParents(List<SuperMetadataBean> list) {
    for (SuperMetadataBean smd : list) {
      smd.setParent(findBestParent(smd, list));
    }
    return list;
  }

  /**
   * Find the parent of a {@link Metadata} from the list, and return it as {@link SuperMetadataBean}
   * . This is Dependent to the statement (if it has a parent statement), and the position of the
   * child in the list
   * 
   * @param md
   * @param list
   * @return
   */
  private SuperMetadataBean findBestParent(SuperMetadataBean child, List<SuperMetadataBean> list) {
    // If the statement has no parent, the metadata doens't as well
    if (child.getStatement().getParent() == null)
      return null;
    // The possible parents
    List<SuperMetadataBean> candidates = new ArrayList<SuperMetadataBean>();
    // Find all candidates
    for (SuperMetadataBean md : list) {
      if (child.getStatement().getParent().compareTo(md.getStatement().getId()) == 0) {
        candidates.add(md);
      }
    }
    // Return the best candidate
    for (int i = candidates.size() - 1; i >= 0; i--) {
      if (candidates.get(i).getPos() < child.getPos()) {
        return candidates.get(i);
      }
    }
    return null;
  }
}
