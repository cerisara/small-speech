package fr.xtof54.jtransapp;

import edu.cmu.sphinx.linguist.acoustic.*;
import java.util.*;

/**
 * Set of unique HMM states.
 */
public class StatePool {

	public static final String SILENCE_PHONE = "SIL";

	/**
	 * Maps a phone to indices of each of its HMM states in the
	 * uniqueStates array.
	 */
	private Map<String, int[]> phoneUStates = new HashMap<>();

	private List<HMMState> uniqueStates = new ArrayList<>();

	private AcousticModel acMod = HMMModels.getAcousticModels();
	private UnitManager unitMgr = new UnitManager();


	public StatePool() {
		clear();
	}


	public void clear() {
		phoneUStates.clear();
		uniqueStates.clear();
	}


	public int add(HMMState state) {
		int stateNo = state.getState();
		String phone = getPhone(state);
		int[] phoneIdxes = phoneUStates.get(phone);

		assert stateNo >= 0 && stateNo < 3;

		if (null == phoneIdxes) {
			phoneIdxes = new int[]{-1, -1, -1};
			phoneUStates.put(phone, phoneIdxes);
		} else if (phoneIdxes[stateNo] >= 0) {
			// already added
			return phoneIdxes[stateNo];
		}

		assert !uniqueStates.contains(state);
		int idx = uniqueStates.size();

		phoneIdxes[stateNo] = idx;
		uniqueStates.add(state);

		return idx;
	}


	public void check(String phone) {
		if (!phoneUStates.containsKey(phone)) {
			addPhone(phone);
		}
	}


	private void addPhone(String phone) {
		assert !phoneUStates.containsKey(phone);

		// find HMM for this phone
		HMM hmm = acMod.lookupNearestHMM(
				unitMgr.getUnit(phone), HMMPosition.UNDEFINED, false);

		for (int i = 0; i < 3; i++) {
			HMMState state = hmm.getState(i);
			add(state);

			assert state.isEmitting();
			assert !state.isExitState();
			assert state.getSuccessors().length == 2;

			for (HMMStateArc arc: state.getSuccessors()) {
				HMMState arcState = arc.getHMMState();
				if (i == 2 && arcState.isExitState()) {
					continue;
				}

				if (arcState != state) {
					assert i != 2;
					assert !arcState.isExitState();
					assert arcState == hmm.getState(i+1);
				}
			}
		}
	}


	public int getId(String phone, int stateNo) {
		if (stateNo < 0 || stateNo > 2) {
			throw new IllegalArgumentException("illegal state number " +
					"(valid state numbers are 0, 1, 2)");
		}

		return phoneUStates.get(phone)[stateNo];
	}


	public HMMState get(int id) {
		return uniqueStates.get(id);
	}


	public int indexOf(HMMState state) {
		return uniqueStates.indexOf(state);
	}


	public int size() {
		return uniqueStates.size();
	}


	public static boolean isSilenceState(HMMState state) {
		return getPhone(state).equals(SILENCE_PHONE);
	}


	public static String getPhone(HMMState state) {
		return null == state
				? "null"
				: state.getHMM().getBaseUnit().getName();
	}

}
