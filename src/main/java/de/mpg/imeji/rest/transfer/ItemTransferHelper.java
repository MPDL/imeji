package de.mpg.imeji.rest.transfer;

import java.io.Serializable;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;

public class ItemTransferHelper implements Serializable {

  private static final long serialVersionUID = 6920337753905863673L;
  private int pos;
  private String key;
  private JsonNode value;
  private String statementID;
  private Entry<String, JsonNode> entry;

  public ItemTransferHelper() {

  }

  public ItemTransferHelper(int pos, String key, JsonNode value, String statementID, Entry<String, JsonNode> entry) {
    this.pos = pos;
    this.key = key;
    this.value = value;
    this.statementID = statementID;
    this.entry = entry;
  }

  public int getPos() {
    return pos;
  }

  public void setPos(int pos) {
    this.pos = pos;
  }

  public String getKey() {
    return key;
  }

  public JsonNode getValue() {
    return value;
  }

  public void setValue(JsonNode value) {
    this.value = value;
  }

  public String getStatementID() {
    return statementID;
  }

  public void setStatementID(String statementID) {
    this.statementID = statementID;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public Entry<String, JsonNode> getEntry() {
    return entry;
  }

  public void setEntry(Entry<String, JsonNode> entry) {
    this.entry = entry;
  }

}
