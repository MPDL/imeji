package de.mpg.imeji.presentation.admin;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Future;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.logic.batch.StorageUsageAnalyseJob;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.factory.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.statistic.StatisticsService;

@ManagedBean
@ViewScoped
public class StorageStatisticsBean {
	private static final Logger LOGGER = LogManager.getLogger(StorageStatisticsBean.class);
	private List<Institute> institutes = new ArrayList<>();
	private String allFileSize;
	private String numberOfFilesInStorage;
	private String sizeOfFilesinStorage;
	private String freeSpaceInStorage;
	private String lastUpdateStorageStatistics;
	private Future<Integer> storageAnalyseStatus;

	public StorageStatisticsBean() {
		try {
			final StatisticsService controller = new StatisticsService();
			for (final String institute : controller.getAllInstitute()) {
				institutes.add(new Institute(institute, controller.getUsedStorageSizeForInstitute(institute)));
			}
			allFileSize = FileUtils.byteCountToDisplaySize(controller.getAllFileSize());
			final StorageUsageAnalyseJob storageUsageAnalyse = new StorageUsageAnalyseJob();
			this.numberOfFilesInStorage = Integer.toString(storageUsageAnalyse.getNumberOfFiles());
			this.sizeOfFilesinStorage = FileUtils.byteCountToDisplaySize(storageUsageAnalyse.getStorageUsed());
			this.freeSpaceInStorage = FileUtils.byteCountToDisplaySize(storageUsageAnalyse.getFreeSpace());
			this.lastUpdateStorageStatistics = storageUsageAnalyse.getLastUpdate();
		} catch (IOException | URISyntaxException e) {
			LOGGER.error("Error constructing StorageUsageAnalyseJob", e);
		}
	}

	/**
	 * Start the job {@link StorageUsageAnalyseJob}
	 *
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public String analyseStorageUsage() throws IOException, URISyntaxException {
		storageAnalyseStatus = Imeji.getEXECUTOR().submit(new StorageUsageAnalyseJob());
		return "";
	}

	/**
	 * return count of all {@link CollectionImeji}
	 *
	 * @return
	 */
	public int getAllCollectionsSize() {
		final Search search = SearchFactory.create(SearchObjectTypes.COLLECTION, SEARCH_IMPLEMENTATIONS.JENA);
		return search.searchString(JenaCustomQueries.selectCollectionAll(), null, null, Search.SEARCH_FROM_START_INDEX,
				Search.GET_ALL_RESULTS).getNumberOfRecords();
	}

	/**
	 * return count of all {@link User}
	 *
	 * @return
	 */
	public int getAllUsersSize() {
		try {
			return new UserService().searchUserByName("").size();
		} catch (final Exception e) {
			return 0;
		}
	}

	/**
	 * return count of all {@link Item}
	 *
	 * @return
	 */
	public int getAllImagesSize() {
		final Search search = SearchFactory.create(SearchObjectTypes.ITEM, SEARCH_IMPLEMENTATIONS.JENA);
		return search.searchString(JenaCustomQueries.selectItemAll(), null, null, Search.SEARCH_FROM_START_INDEX,
				Search.GET_ALL_RESULTS).getNumberOfRecords();
	}

	public ArrayList<Institute> getInstitutes() {
		Collections.sort(institutes, new Comparator<Institute>() {
			@Override
			public int compare(Institute institute1, Institute institute2) {
				if ((institute2.getStorage() - institute1.getStorage()) >= 0) {
					return 1;
				} else {
					return -1;
				}
			}
		});
		return (ArrayList<Institute>) institutes;

	}

	public String getAllFileSize() {
		return allFileSize;
	}

	public String getNumberOfFilesInStorage() {
		return numberOfFilesInStorage;
	}

	public String getSizeOfFilesinStorage() {
		return sizeOfFilesinStorage;
	}

	public String getFreeSpaceInStorage() {
		return freeSpaceInStorage;
	}

	public String getLastUpdateStorageStatistics() {
		return lastUpdateStorageStatistics;
	}

	public boolean getStorageAnalyseStatus() {
		if (storageAnalyseStatus != null) {
			return storageAnalyseStatus.isDone();
		}
		return true;
	}

}
