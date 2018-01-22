package de.mpg.imeji.logic.model;

import java.util.stream.Stream;

/**
 * All indexes names, searchable in imeji
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public enum SearchFields {
  id, collection, collectionid, creatorid, creator, collaborator, role, read, editor, created, modified, status, filename, col, family, given, organization, title, description, author, author_familyname(
      "author.familyname"), author_givenname("author.givenname"), author_organization(
          "author.organization"), md, index, all, license, checksum, filetype, filesize, pid, info_label, info_text, info_url, email, technical, fulltext, completename, collection_title(
              "collection.title"), collection_description(
                  "collection.description"), collection_author(
                      "collection.author"), collection_author_organisation(
                          "collection.author.organization");

  private final String searchIndex;

  private SearchFields() {
    // index is same as name
    searchIndex = this.name().toLowerCase();
  }

  private SearchFields(String index) {
    this.searchIndex = index;
  }

  public String getIndex() {
    return searchIndex;
  }

  public static SearchFields valueOfIndex(String index) {
    return Stream.of(SearchFields.values()).filter(f -> f.getIndex().equals(index.toLowerCase()))
        .findFirst().orElse(all);
  }
}
