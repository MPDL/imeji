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
package de.mpg.imeji.rest.to;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.mpg.imeji.logic.util.MetadataAndProfileHelper;
import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.rest.helper.MetadataTransferHelper;

@JsonInclude(Include.NON_NULL)
/**
 * Implements Hierarchy for a {@link List} of {@link SuperMetadataBeanTOTO}
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SuperMetadataTreeTO implements Serializable {
  private static final long serialVersionUID = 8726381279225329881L;
  private Map<String, SuperMetadataBeanTO> map = new LinkedHashMap<String, SuperMetadataBeanTO>();
  // private Map<String, List<JsonNode>> oMap = new LinkedHashMap<String, List<JsonNode>>();
  private Map<String, JsonNode> oMap = new LinkedHashMap<String, JsonNode>();
  private MetadataProfile profile;
  private ObjectMapper mapper = new ObjectMapper();
  private static final Logger LOGGER = Logger.getLogger(SuperMetadataTreeTO.class);

  /**
   * Default Constructor
   */

  public SuperMetadataTreeTO(Collection<Metadata> metadataSet, MetadataProfile profile) {

    List<SuperMetadataBeanTO> list = new ArrayList<SuperMetadataBeanTO>();
    for (Metadata md : metadataSet) {

      Statement st = MetadataAndProfileHelper.getStatement(md.getStatement(), profile);

      if (st != null) {
        SuperMetadataBeanTO smd = new SuperMetadataBeanTO(md, st);
        list.add(smd);
      }
    }

    Collections.sort(list);
    // Set parents according to the current order
    list = setParents(list);
    // Create the map
    this.profile = profile;
    map = createMap(list, null, "0");

    // Set the children
    for (String mapLabel : map.keySet()) {
      SuperMetadataBeanTO smb = map.get(mapLabel);
      smb.setChilds(getChilds(smb.getTreeIndex()));
    }

    for (SuperMetadataBeanTO smb : list) {
      setJsonFieldParent(smb);
      if (!smb.getTreeIndex().contains(",")) {
        addOMap(smb.getLabel(),
            mapper.valueToTree(smb.isMultiple() ? smb.getJsonField() : smb.getJsonFieldSingle()),
            smb.isMultiple());
      }
    }
  }



  /**
   * Create the {@link LinkedList} of {@link Metadata} with template (default values) from the
   * {@link MetadataProfile} For each profile statement, it will generate a default value, depending
   * on the metadata type It returns a generated "metadata record", which can be serialized as JSON
   * as any other metadata
   * 
   * @param profile
   * @return
   */
  public static LinkedList<Metadata> generateDefaultValues(MetadataProfile profile) {
    LinkedList<Metadata> mdC = new LinkedList<Metadata>();
    int i = 0;
    for (Statement st : profile.getStatements()) {
      Metadata md;
      try {
        md = MetadataAndProfileHelper.getDefaultValueMetadata(st);
        md.setPos(i);
        md.setStatement(st.getId());
        mdC.add(md);
        i++;
      } catch (Exception e) {
        LOGGER.error("Error generating default value for metadata", e);
      }
    }
    return mdC;
  }

  /**
   * Create the {@link Map} from the {@link List}
   * 
   * @param list
   * @param parent
   * @param index
   * @return
   */
  private Map<String, SuperMetadataBeanTO> createMap(List<SuperMetadataBeanTO> list,
      SuperMetadataBeanTO parent, String index) {
    Map<String, SuperMetadataBeanTO> map = new LinkedHashMap<String, SuperMetadataBeanTO>();
    for (SuperMetadataBeanTO smd : list) {
      if ((parent == null && isRootMetadata(smd)) || isChild(smd, parent)) {
        smd.setTreeIndex(index);
        map.put(smd.getTreeIndex(), smd);
        map.putAll(createMap(removeFromList(list, smd), smd, addIndex(index, "0")));
        index = incrementIndex(index);
        // set here the JsonField & Value of Root data
        setJsonFieldRootAndLeave(smd);
      }
    }
    return map;
  }

  /**
   * Create the {@link Map} from the {@link List}
   * 
   * @param list
   * @param parent
   * @param index
   * @return
   */
  private void setJsonFieldRootAndLeave(SuperMetadataBeanTO smd) {

    if (!MetadataAndProfileHelper.isMetadataParent(smd.getStatement().getId(), getProfile())) {

      JsonNode jsonVal =
          MetadataTransferHelper.serializeMetadata(smd.getMetadata(), smd.getStatement());

      List<JsonNode> allVals = (new ArrayList<JsonNode>());
      allVals.add(jsonVal);
      smd.setJsonFieldList(allVals);
    }

  }

  private void addOMap(String label, JsonNode value, boolean isMultiple) {
    ArrayNode multipleVal = mapper.createArrayNode();
    if (oMap.get(label) == null) {
      oMap.put(label, isMultiple ? multipleVal.add(value.get(0)) : value);
    } else {
      JsonNode mapNode = oMap.get(label);
      if (mapNode.isArray()) {
        ((ArrayNode) mapNode).add(value.get(0));
      }
      oMap.put(label, mapNode);
    }
  }

  private void setJsonFieldParent(SuperMetadataBeanTO parent) {

    if (!MetadataAndProfileHelper.isMetadataParent(parent.getStatement().getId(), profile)) {
      return;
    }

    // if metadata is not parent, then return, otherwise,
    // produce proper Json even if there are no children values

    List<JsonNode> parentValues = new ArrayList<JsonNode>();
    if (parent.isMultiple() && parent.getJsonField() != null) {
      parentValues = parent.getJsonField();
    }

    if (!parent.isMultiple() && parent.getJsonFieldSingle() != null) {
      parentValues.add(parent.getJsonFieldSingle());
    }


    ObjectNode parentNode = mapper.createObjectNode();
    if (parentValues.size() == 0) {
      JsonNode jsonParentValue =
          MetadataTransferHelper.serializeMetadata(parent.getMetadata(), parent.getStatement());
      List<JsonNode> jsonParentList = new ArrayList<JsonNode>();
      jsonParentList.add(jsonParentValue);
      // CHANGE LIST/MULTIPLE
      parentNode.set(parent.getLabel(), mapper.valueToTree(jsonParentList.get(0)));
    }

    // Map<String, List<JsonNode>> childrenMap = new LinkedHashMap<String, List<JsonNode>>();
    // CHANGE LIST/MULTIPLE
    Map<String, JsonNode> childrenMap = new LinkedHashMap<String, JsonNode>();

    for (SuperMetadataBeanTO smbchild : parent.getChilds()) {

      if (smbchild.getParent().getTreeIndex().equals(parent.getTreeIndex())) {

        setJsonFieldParent(smbchild);

        if (childrenMap.get(smbchild.getLabel()) == null
        // change LIST/MULTIPLE || childrenMap.get(smbchild.getLabel()).isEmpty()
        ) {
          childrenMap.put(smbchild.getLabel(), mapper.valueToTree(
              smbchild.isMultiple() ? smbchild.getJsonField() : smbchild.getJsonFieldSingle()));
        } else {

          if (smbchild.isMultiple()) {
            ArrayNode childVals = (ArrayNode) childrenMap.get(smbchild.getLabel());
            childVals.addAll(smbchild.getJsonField());
            childrenMap.put(smbchild.getLabel(), childVals);
          } else {
            childrenMap.put(smbchild.getLabel(), smbchild.getJsonFieldSingle());
          }
        }

      }

    }

    for (String label : childrenMap.keySet()) {
      parentNode.set(label, mapper.valueToTree(childrenMap.get(label)));
    }


    parentValues.add(parentNode);
    parent.setJsonFieldList(parentValues);
  }


  /**
   * True if the {@link SuperMetadataBeanTO} is a root metadata
   * 
   * @param smb
   * @return
   */
  private boolean isRootMetadata(SuperMetadataBeanTO smb) {
    return smb.getParent() == null;
  }


  /**
   * True if the {@link SuperMetadataBeanTO} smb is child of parent
   * 
   * @param smb
   * @param parent
   * @return
   */
  private boolean isChild(SuperMetadataBeanTO smb, SuperMetadataBeanTO parent) {
    if (parent == null || smb.getParent() == null) {
      return false;
    } else {
      return smb.getParent().getMetadata().getId().compareTo(parent.getMetadata().getId()) == 0;
    }
  }

  /**
   * Remove the {@link SuperMetadataBeanTO} smb from the {@link List}. A new list is returned, which
   * allows to use this method within the for loop on the list elements
   * 
   * @param list
   * @param smb
   * @return
   */
  private List<SuperMetadataBeanTO> removeFromList(List<SuperMetadataBeanTO> list,
      SuperMetadataBeanTO smb) {
    List<SuperMetadataBeanTO> subList = new ArrayList<SuperMetadataBeanTO>(list);
    subList.remove(smb);
    return subList;
  }


  /**
   * Get the childs of tree element
   * 
   * @param parentIndex
   * @return
   */
  public List<SuperMetadataBeanTO> getChilds(String parentIndex) {
    List<SuperMetadataBeanTO> childs = new ArrayList<SuperMetadataBeanTO>();
    for (SuperMetadataBeanTO smb : map.values()) {
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
   * Set the Parent {@link SuperMetadataBeanTO} to all element of the list. This is made according
   * to the current order of the list (see findBestParent() method)
   * 
   * @param flat
   * @return
   */
  private List<SuperMetadataBeanTO> setParents(List<SuperMetadataBeanTO> list) {
    for (SuperMetadataBeanTO smd : list) {
      smd.setParent(findBestParent(smd, list));
    }
    return list;
  }

  /**
   * Find the parent of a {@link Metadata} from the list, and return it as
   * {@link SuperMetadataBeanTO} . This is Dependent to the statement (if it has a parent
   * statement), and the position of the child in the list
   * 
   * @param md
   * @param list
   * @return
   */
  private SuperMetadataBeanTO findBestParent(SuperMetadataBeanTO child,
      List<SuperMetadataBeanTO> list) {
    // If the statement has no parent, the metadata doens't as well
    if (child.getStatement().getParent() == null)
      return null;
    // The possible parents
    List<SuperMetadataBeanTO> candidates = new ArrayList<SuperMetadataBeanTO>();
    // Find all candidates
    for (SuperMetadataBeanTO md : list) {
      if (child.getStatement().getParent().compareTo(md.getStatement().getId()) == 0)
        candidates.add(md);
    }
    // Return the best candidate
    for (int i = candidates.size() - 1; i >= 0; i--) {
      if (candidates.get(i).getPos() < child.getPos()) {
        return candidates.get(i);
      }
    }
    return null;
  }


  public Map<String, SuperMetadataBeanTO> getMap() {
    return map;
  }

  public Map<String, JsonNode> getOMap() {

    return oMap;
  }

  public Map<String, JsonNode> getOMapWithJsonNodes() {
    Map<String, JsonNode> json = new LinkedHashMap<String, JsonNode>();
    for (String label : getOMap().keySet()) {
      json.put(label, mapper.valueToTree(getOMap().get(label)));
    }

    return json;
  }


  /**
   * @return the profile
   */
  public MetadataProfile getProfile() {
    return profile;
  }

  /**
   * @param profile the profile to set
   */
  public void setProfile(MetadataProfile profile) {
    this.profile = profile;
  }
}
