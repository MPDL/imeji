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

import de.mpg.imeji.logic.util.MetadataFactory;
import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.MetadataSet;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.presentation.metadata.util.MetadataHelper;
import de.mpg.imeji.presentation.util.ProfileHelper;

/**
 * The Java Bean for a {@link MetadataSet}
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class MetadataSetBean implements Serializable {

  private static final long serialVersionUID = 3681048029394470353L;
  private MetadataProfile profile = null;
  private SuperMetadataTree metadataTree = null;
  private SuperMetadataTree uncutTree = null;
  private static final Logger LOGGER = Logger.getLogger(MetadataSetBean.class);

  /**
   * Constructor for a {@link MetadataSet}
   * 
   * @param mds
   * @param addEmtpyValue if true, add an emtpy metadata for all {@link Statement} which don't have
   *        any value
   */
  public MetadataSetBean(MetadataSet mds, MetadataProfile profile, boolean addEmtpyValue) {
    this.profile = profile;
    // Init the list of metadata
    initTreeFromList(toSuperList((List<Metadata>) mds.getMetadata()));
    if (profile != null && addEmtpyValue) {
      addEmtpyValues();
    }
  }

  /**
   * Initialize the {@link MetadataSetBean} with a flat {@link List} of {@link SuperMetadataBean}
   * 
   * @param flat
   */
  public void initTreeFromList(List<SuperMetadataBean> list) {
    metadataTree = new SuperMetadataTree(list);
  }

  /**
   * Remove all Emtpy {@link SuperMetadataBean} of the {@link MetadataSetBean} (if and only if the
   * {@link SuperMetadataBean} doesn't have a child)
   */
  public void trim() {
    setUncutTree(new SuperMetadataTree(metadataTree.getList()));
    for (SuperMetadataBean smb : metadataTree.getList()) {
      if (isHierarchyEmpty(smb)) {
        metadataTree.removeButKeepChilds(smb);
      }
    }
  }

  private boolean isHierarchyEmpty(SuperMetadataBean smb) {
    if (smb.isEmpty() && smb.getChilds().size() == 0) {
      return true;
    } else if (!smb.isEmpty()) {
      return false;
    } else {
      boolean empty = true;
      for (SuperMetadataBean child : smb.getChilds()) {
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
    metadataTree.add(new SuperMetadataBean(MetadataFactory.createMetadata(st), st));
  }

  /**
   * Create a {@link List} of {@link SuperMetadataBean} from a list of {@link Metadata}
   * 
   * @return
   */
  private List<SuperMetadataBean> toSuperList(List<Metadata> l) {
    List<SuperMetadataBean> flat = new ArrayList<SuperMetadataBean>();
    for (Metadata md : l) {
      Statement st = ProfileHelper.getStatement(md.getStatement(), profile);
      if (st != null) {
        SuperMetadataBean smd = new SuperMetadataBean(md, st);
        flat.add(smd);
      } else {
        LOGGER.error("A metadata " + md.getId()
            + " is defined with a non existing statement in profile " + this.profile.getId());
      }
    }
    return flat;
  }

  /**
   * Add to the {@link SuperMetadataTree} new emtpy {@link SuperMetadataBean} when it hadn't been
   * already defined one. this method does the contrary as the trim() method
   */
  public void addEmtpyValues() {
    // Find the Metadata which have not been defined with a value into the
    // Metadataset
    List<SuperMetadataBean> l = new ArrayList<SuperMetadataBean>(metadataTree.getList());

    // check if profile statements exist at least once and add missing statements
    int lastRootIndex = 0;
    int rootPos = 0;
    for (Statement st : profile.getStatements()) {
      if (!exists(st)) {
        SuperMetadataBean smb = new SuperMetadataBean(MetadataFactory.createMetadata(st), st);
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
    for (SuperMetadataBean smb : l) {
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
            SuperMetadataBean newSmb =
                new SuperMetadataBean(MetadataFactory.createMetadata(profile.getStatement(childId)),
                    profile.getStatement(childId));
            statementOccurrences.put(childId, statementOccurrences.get(childId) + 1);
            l.add(newSmb);
          }

        }
      }
    }


    // find parent for new elements from top to bottom
    for (SuperMetadataBean b : l) {
      if (b.getTreeIndex().equals("")) {
        for (SuperMetadataBean parent : l) {
          boolean parentFound = false;
          if (parent.getStatement().getId().toString()
              .equals(b.getStatement().getParent().toString())) {
            boolean hasChild = false;
            int pos = 0;
            for (SuperMetadataBean smb : parent.getChilds()) {
              pos++;
              if (smb.getStatementId().equals(b.getStatementId())) {
                hasChild = true;
              }
            }
            if (!hasChild) {
              b.setParent(parent);
              parent.getChilds().add(b);
              b.setTreeIndex(parent.getTreeIndex() + "," + pos);
              for (SuperMetadataBean smb2 : l) {
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

  public int countStatementOccurrence(List<SuperMetadataBean> l, String statementID) {
    int count = 0;
    for (SuperMetadataBean md : l) {
      if (md.getStatement().getId().toString().equals(statementID))
        count++;
    }
    return count;
  }

  private SuperMetadataBean getlastStatementofType(List<SuperMetadataBean> l, String string) {
    for (int i = l.size() - 1; i >= 0; i--) {
      if (l.get(i).getStatement().getId().toString().equals(string)) {
        return l.get(i);
      }
    }
    return null;
  }

  /**
   * True if the {@link MetadataSetBean} has one {@link Metadata} with this {@link Statement}
   * 
   * @param st
   * @return
   */
  public boolean exists(Statement st) {
    for (SuperMetadataBean md : metadataTree.getList()) {
      if (md.getStatement().getId().compareTo(st.getId()) == 0)
        return true;
    }
    return false;
  }

  /**
   * True if the {@link MetadataSetBean} has one {@link Metadata} with this {@link Statement} which
   * is not emtpy
   * 
   * @param st
   * @return
   */
  public boolean existsNotEmtpy(Statement st) {
    for (SuperMetadataBean md : metadataTree.getList()) {
      if (!MetadataHelper.isEmpty(md.getMetadata()))
        if (md.getStatement().getId().compareTo(st.getId()) == 0
            || ProfileHelper.isParent(st, md.getStatement(), profile))
          return true;
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
  public SuperMetadataTree getTree() {
    return metadataTree;
  }

  /**
   * @param metadataTree the metadataTree to set
   */
  public void setTree(SuperMetadataTree metadataTree) {
    this.metadataTree = metadataTree;
  }

  public SuperMetadataTree getUncutTree() {
    return uncutTree;
  }

  public void setUncutTree(SuperMetadataTree uncutTree) {
    this.uncutTree = uncutTree;
  }
}
