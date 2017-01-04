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
package de.mpg.imeji.logic.storage;


/**
 * Result of an Upload
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class UploadResult {
  private String id;
  private String orginal;
  private String full;
  private String web;
  private String thumb;
  private String checksum;
  private String status;
  private long fileSize;
  private long width;
  private long height;

  /**
   * Default constructor
   */
  public UploadResult() {

  }

  /**
   * Constructor with the 3 results
   *
   * @param orginal
   * @param web
   * @param thumb
   */
  public UploadResult(String id, String orginal, String web, String thumb, String full) {
    this.id = id;
    this.orginal = orginal;
    this.thumb = thumb;
    this.web = web;
    this.full = full;
  }

  public String getOrginal() {
    return orginal;
  }

  public void setOrginal(String orginal) {
    this.orginal = orginal;
  }

  public String getWeb() {
    return web;
  }

  public void setWeb(String web) {
    this.web = web;
  }

  public String getThumb() {
    return thumb;
  }

  public void setThumb(String thumb) {
    this.thumb = thumb;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  /**
   * @param checksum the checksum to set
   */
  public void setChecksum(String checksum) {
    this.checksum = checksum;
  }

  /**
   * @return the checksum
   */
  public String getChecksum() {
    return checksum;
  }

  /**
   * @return the status
   */
  public String getStatus() {
    return status;
  }

  /**
   * @param status the status to set
   */
  public void setStatus(String status) {
    this.status = status;
  }

  public long getFileSize() {
    return fileSize;
  }

  public void setFileSize(long fileSize) {
    this.fileSize = fileSize;
  }

  public long getWidth() {
    return width;
  }

  public void setWidth(long width) {
    this.width = width;
  }

  public long getHeight() {
    return height;
  }

  public void setHeight(long height) {
    this.height = height;
  }

  public String getFull() {
    return full;
  }

  public void setFull(String full) {
    this.full = full;
  }
}
