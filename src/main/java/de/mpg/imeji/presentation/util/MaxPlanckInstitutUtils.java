package de.mpg.imeji.presentation.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Utility Class for specific method for the Max Planck Institute
 * 
 * @author saquet
 *
 */
public class MaxPlanckInstitutUtils {
  private static final Logger LOGGER = Logger.getLogger(MaxPlanckInstitutUtils.class);

  /**
   * URL of the file with IP range information for all MPIs
   */
  private static String MAX_PLANCK_INSTITUTES_IP_URL =
      "http://www2.mpdl.mpg.de/seco-irmapubl/expoipra_mpi?style=expo01";
  private static int TIME_OUT = 5000;
  /**
   * Map of the institute names with their IP range
   */
  private static Map<String, String> MPINameMap;
  /**
   * Map the institute Id with their ID
   */
  private static Map<String, String> IdMap;

  /**
   * Private Constructor
   */
  private MaxPlanckInstitutUtils() {}

  /**
   * Return the name of the Max Planck Institute according to the IP. If not found, return null
   * 
   * @param ip
   * @return
   */
  public static String getInstituteNameForIP(String userIP) {
    if (MPINameMap != null) {
      try {
        for (String ipRange : MPINameMap.keySet()) {
          if (IPUtils.isInRange(ipRange, userIP)) {
            return MPINameMap.get(ipRange);
          }
        }
      } catch (Exception e) {
        LOGGER.error("Error reading the institute name", e);
      }
    }
    return null;
  }

  /**
   * Return the Id of the
   * 
   * @param userIP
   * @return
   */
  public static String getInstituteIdForIP(String userIP) {
    if (IdMap != null) {
      try {
        for (String ipRange : IdMap.keySet()) {
          if (IPUtils.isInRange(ipRange, userIP)) {
            return IdMap.get(ipRange);
          }
        }
      } catch (Exception e) {
        LOGGER.error("Error reading the institute Id", e);
      }
    }
    return null;
  }

  /**
   * Get the file with the the mpis and their IPs, and return it as a {@link Map}
   * 
   * @return
   */
  public static void initMPINameMap() {
    try {
      LOGGER.info("Reading MPG Institute IP Mapping by name");
      MPINameMap = readMPIMap(1, 4);
    } catch (Exception e) {
      LOGGER.error("There was a problem with finding the MPINameMap: " + e.getLocalizedMessage());
    }
  }

  /**
   * Get the file with the the mpis and their IPs, and return it as a {@link Map}
   * 
   * @return
   */
  public static void initIdMap() {
    try {
      LOGGER.info("Reading MPG Institute IP Mapping by ID");
      IdMap = readMPIMap(1, 2);
    } catch (Exception e) {
      LOGGER.error("There was a problem with finding the IdMap: " + e.getLocalizedMessage());
    }
  }

  /**
   * Return a map from the file MAX_PLANCK_INSTITUTES_IP_URL with the chosen key/value. <br/>
   * The format of the file is following: <br/>
   * line;ip_range;institute_id;institute_city;institute_name
   * 
   * @return
   */
  private static Map<String, String> readMPIMap(int keyPosition, int valuePosition) {
    try {
      URL mpiCSV = new URL(MAX_PLANCK_INSTITUTES_IP_URL);
      URLConnection con = mpiCSV.openConnection();
      con.setConnectTimeout(TIME_OUT);
      BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
      String line = "";
      String cvsSplitBy = ";";
      Map<String, String> maps = new HashMap<String, String>();
      while ((line = br.readLine()) != null) {
        // use comma as separator
        String[] institute = line.split(cvsSplitBy);
        maps.put(institute[keyPosition], institute[valuePosition]);
      }
      return maps;

    } catch (Exception e) {
      LOGGER.error("There was a problem by getting the MPI Map: (" + e.getLocalizedMessage()
          + ")! Check your internet connection. ");
      return null;
    }
  }

}
