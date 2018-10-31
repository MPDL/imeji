package de.mpg.imeji.logic.search.elasticsearch;

import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;

import de.mpg.imeji.logic.search.Search.SearchObjectTypes;

/**
 * elasticsearch service for spot
 *
 * @author bastiens
 *
 */
public class ElasticService {
	private static Node NODE;
	private static Client client;
	static String CLUSTER_NAME = "name of my cluster";
	static String CLUSTER_DIR = "null";
	public static ElasticAnalysers ANALYSER;
	static final String SETTINGS_DEFAULT = "elasticsearch/Settings_default.json";
	static final String SETTINGS_DUCET = "elasticsearch/Settings_ducet.json";

	/**
	 * The Index where all data are indexed
	 */
	public static String DATA_ALIAS = "data";

	/**
	 * The Types in Elasticsearch
	 *
	 * @author bastiens
	 *
	 */
	public enum ElasticTypes {
		content, items, folders, users, usergroups;

		/**
		 * Map a {@link SearchObjectTypes} to an ElasticTypes
		 * 
		 * @param type
		 * @return
		 */
		public static ElasticTypes toElasticTypes(SearchObjectTypes type) {
			switch (type) {
				case ITEM :
					return ElasticTypes.items;
				case COLLECTION :
					return ElasticTypes.folders;
				case USER :
					return ElasticTypes.users;
				case USERGROUPS :
					return ElasticTypes.usergroups;
				case CONTENT :
					return ElasticTypes.content;
				default :
					return ElasticTypes.items;
			}
		}
	}

	public enum ElasticAnalysers {
		standard, ducet_sort, simple, keyword;
	}

	public static Client getClient() {
		return client;
	}

	public static void setClient(Client client) {
		ElasticService.client = client;
	}

	static Node getNODE() {
		return NODE;
	}

	static void setNODE(Node nODE) {
		NODE = nODE;
	}
}
