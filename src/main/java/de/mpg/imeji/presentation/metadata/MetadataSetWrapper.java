/*
 *
 * CDDL HEADER START
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.controller.util.MetadataProfileUtil;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.MetadataSet;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.predefinedMetadata.Metadata;
import de.mpg.imeji.logic.vo.util.MetadataAndProfileHelper;
import de.mpg.imeji.logic.vo.util.MetadataFactory;
import de.mpg.imeji.presentation.metadata.util.MetadataHelper;

/**
 * The Java Bean for a {@link MetadataSet}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class MetadataSetWrapper implements Serializable {

  private static final long serialVersionUID = 3681048029394470353L;
  private MetadataProfile profile = null;
  private MetadataWrapperTree metadataTree = null;
  private MetadataWrapperTree uncutTree = null;
  private static final Logger LOGGER = Logger.getLogger(MetadataSetWrapper.class);

  /**
   * Constructor for a {@link MetadataSet}
   *
   * @param mds
   * @param addEmtpyValue if true, add an emtpy metadata for all {@link Statement} which don't have
   *        any value
   */
  public MetadataSetWrapper(MetadataSet mds, MetadataProfile profile, boolean addEmtpyValue) {
    this.profile = profile;
    // Init the list of metadata
    initTreeFromList(toSuperList((List<Metadata>) mds.getMetadata()));
    if (profile != null && addEmtpyValue) {
      addEmtpyValues();
    }
  }

  /**
   * Initialize the {@link MetadataSetWrapper} with a flat {@link List} of {@link MetadataWrapper}
   *
   * @param flat
   */
  public void initTreeFromList(List<MetadataWrapper> list) {
    metadataTree = new MetadataWrapperTree(list);
  }

  /**
   * Remove all Emtpy {@link MetadataWrapper} of the {@link MetadataSetWrapper} (if and only if the
   * {@link MetadataWrapper} doesn't have a child)
   */
  public void trim() {
    setUncutTree(new MetadataWrapperTree(metadataTree.getList()));
    for (MetadataWrapper smb : metadataTree.getList()) {
      if (isHierarchyEmpty(smb)) {
        metadataTree.removeButKeepChilds(smb);
      }
    }
  }

  private boolean isHierarchyEmpty(MetadataWrapper smb) {
    if (smb.isEmpty() && smb.getChilds().size() == 0) {
      return true;
    } else if (!smb.isEmpty()) {
      return false;
    } else {
      boolean empty = true;
      for (MetadataWrapper child : smb.getChilds()) {
        empty = isHierarchyEmpty(child);
      }
      return empty;
    }
  }

  /**
   * Add a new emtpy {@link Metadata} for the passed statement
   *
   * @param st
   */
  public void appendEmtpyMetadata(Statement st) {
    metadataTree.add(new MetadataWrapper(MetadataFactory.createMetadata(st), st));
  }

  /**
   * Create a {@link List} of {@link MetadataWrapper} from a list of {@link Metadata}
   *
   * @return
   */
  private List<MetadataWrapper> toSuperList(List<Metadata> l) {
    List<MetadataWrapper> flat = new ArrayList<MetadataWrapper>();
    for (Metadata md : l) {
      Statement st = MetadataAndProfileHelper.getStatement(md.getStatement(), profile);
      if (st != null) {
        MetadataWrapper smd = new MetadataWrapper(md, st);
        flat.add(smd);
      } else {
        LOGGER.error("A metadata " + md.getId()
            + " is defined with a non existing statement in profile " + this.profile.getId());
      }
    }
    return flat;
  }

  /**
   * Add to the {@link MetadataWrapperTree} new emtpy {@link MetadataWrapper} when it hadn't been
   * already defined one. this method does the contrary as the trim() method
   */
  public void addEmtpyValues() {
    // Find the Metadata which have not been defined with a value into the
    // Metadataset
    List<MetadataWrapper> l = new ArrayList<MetadataWrapper>(metadataTree.getList());

    // check if profile statements exist at least once and add missing statements
    int lastRootIndex = 0;
    int rootPos = 0;
    for (Statement st : profile.getStatements()) {
      if (!exists(st)) {
        MetadataWrapper smb = new MetadataWrapper(MetadataFactory.createMetadata(st), st);
        if (smb.getStatement().getParent() == null) {
          smb.setTreeIndex(String.valueOf(lastRootIndex));
          smb.setPos(rootPos);
          rootPos++;
          lastRootIndex++;
        }
        l.add(smb);
      } else {
        rootPos++;
        if (st.getParent() == null) {
          String test = getlastStatementofType(l, st.getId().toString()).getTreeIndex();
          lastRootIndex = Integer.valueOf(test) + 1;
        }
      }
    }

    // Create Map with multipleValue statements
    HashMap<String, Integer> multiValueStatements = new HashMap<String, Integer>();
    for (Statement st : profile.getStatements()) {
      if (st.getMaxOccurs().equals("unbounded")) {
        multiValueStatements.put(st.getId().toString(), 0);
      }
    }

    // count occurance of different multipleValue statements
    for (int i = 0; i < l.size(); i++) {
      String statementId = l.get(i).getStatement().getId().toString();
      if (multiValueStatements.containsKey(statementId)) {
        multiValueStatements.put(statementId, multiValueStatements.get(statementId) + 1);
      }
    }

    // save number of different statements with different parents in HashMap to calculate how many
    // empty statements are needed
    HashSet<String> parents = new HashSet<String>();
    HashMap<String, Integer> statementOccurrences = new HashMap<String, Integer>();
    for (MetadataWrapper smb : l) {
      String statementId = smb.getStatement().getId().toString();
      if (smb.getParent() == null || !(parents.contains(smb.getParent() + statementId))) {
        if (statementOccurrences.containsKey(statementId)) {
          statementOccurrences.put(statementId, statementOccurrences.get(statementId) + 1);
        } else {
          statementOccurrences.put(statementId, 1);
        }
        if (smb.getParent() != null) {
          parents.add(smb.getParent() + statementId);
        }
      }
    }

    // add statements for children of multipleValue statements
    for (Statement st : profile.getStatements()) {
      if (st.getParent() != null) {
        String childId = st.getId().toString();
        String parentId = st.getParent().toString();
        if (multiValueStatements.containsKey(parentId)) {
          while (multiValueStatements.get(parentId) - statementOccurrences.get(childId) > 0) {
            if (multiValueStatements.containsKey(childId)) {
              multiValueStatements.put(childId, multiValueStatements.get(childId) + 1);
            }
            MetadataWrapper newSmb =
                new MetadataWrapper(MetadataFactory.createMetadata(profile.getStatement(childId)),
                    profile.getStatement(childId));
            statementOccurrences.put(childId, statementOccurrences.get(childId) + 1);
            l.add(newSmb);
          }

        }
      }
    }


    // find parent for new elements from top to bottom
    for (MetadataWrapper b : l) {
      if (b.getTreeIndex().equals("")) {
        for (MetadataWrapper parent : l) {
          boolean parentFound = false;
          if (parent.getStatement().getId().toString()
              .equals(b.getStatement().getParent().toString())) {
            boolean hasChild = false;
            int pos = 0;
            for (MetadataWrapper smb : parent.getChilds()) {
              pos++;
              if (smb.getStatementId().equals(b.getStatementId())) {
                hasChild = true;
              }
            }
            if (!hasChild) {
              b.setParent(parent);
              parent.getChilds().add(b);
              b.setTreeIndex(parent.getTreeIndex() + "," + pos);
              for (MetadataWrapper smb2 : l) {
                if (smb2.getPos() > parent.getPos()) {
                  smb2.setPos(smb2.getPos() + 1);
                }
              }
              b.setPos(parent.getPos() + 1);
              parentFound = true;
              break;
            }
          }
          if (parentFound) {
            break;
          }
        }
      }
    }



    // Reinit the tree
    initTreeFromList(l);
  }

  public int countStatementOccurrence(List<MetadataWrapper> l, String statementID) {
    int count = 0;
    for (MetadataWrapper md : l) {
      if (md.getStatement().getId().toString().equals(statementID)) {
        count++;
      }
    }
    return count;
  }

  private MetadataWrapper getlastStatementofType(List<MetadataWrapper> l, String string) {
    for (int i = l.size() - 1; i >= 0; i--) {
      if (l.get(i).getStatement().getId().toString().equals(string)) {
        return l.get(i);
      }
    }
    return null;
  }

  /**
   * True if the {@link MetadataSetWrapper} has one {@link Metadata} with this {@link Statement}
   *
   * @param st
   * @return
   */
  public boolean exists(Statement st) {
    for (MetadataWrapper md : metadataTree.getList()) {
      if (md.getStatement().getId().compareTo(st.getId()) == 0) {
        return true;
      }
    }
    return false;
  }

  /**
   * True if the {@link MetadataSetWrapper} has one {@link Metadata} with this {@link Statement}
   * which is not emtpy
   *
   * @param st
   * @return
   */
  public boolean existsNotEmtpy(Statement st) {
    for (MetadataWrapper md : metadataTree.getList()) {
      if (!MetadataHelper.isEmpty(md.getMetadata())) {
        if (md.getStatement().getId().compareTo(st.getId()) == 0
            || MetadataProfileUtil.isParent(st, md.getStatement(), profile)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * getter
   *
   * @return the profile
   */
  public MetadataProfile getProfile() {
    return profile;
  }

  /**
   * setter
   *
   * @param profile the profile to set
   */
  public void setProfile(MetadataProfile profile) {
    this.profile = profile;
  }

  /**
   * @return the metadataTree
   */
  public MetadataWrapperTree getTree() {
    return metadataTree;
  }

  /**
   * @param metadataTree the metadataTree to set
   */
  public void setTree(MetadataWrapperTree metadataTree) {
    this.metadataTree = metadataTree;
  }

  public MetadataWrapperTree getUncutTree() {
    return uncutTree;
  }

  public void setUncutTree(MetadataWrapperTree uncutTree) {
    this.uncutTree = uncutTree;
  }
}
