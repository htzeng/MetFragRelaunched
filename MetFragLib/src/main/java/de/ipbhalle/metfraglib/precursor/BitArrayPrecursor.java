package de.ipbhalle.metfraglib.precursor;

import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IRingSet;
import org.openscience.cdk.ringsearch.AllRingsFinder;

import de.ipbhalle.metfraglib.BitArray;
import de.ipbhalle.metfraglib.additionals.Bond;
import de.ipbhalle.metfraglib.exceptions.AtomTypeNotKnownFromInputListException;
import de.ipbhalle.metfraglib.fragment.DefaultBitArrayFragment;

public class BitArrayPrecursor extends DefaultPrecursor {

	protected java.util.Vector<short[]> atomIndexToConnectedAtomIndeces;
	protected short[][] bondIndexToConnectedAtomIndeces;
	protected BitArray[] ringBondToBelongingRingBondIndeces;
	protected BitArray aromaticBonds;
	protected short[] atomAdjacencyList;
	
	public BitArrayPrecursor(IAtomContainer precursorMolecule) {
		super(precursorMolecule);
	}
	
	@Override
	public void preprocessPrecursor() throws AtomTypeNotKnownFromInputListException, Exception {
		super.preprocessPrecursor();
		this.initiliseAtomIndexToConnectedAtomIndeces();
		this.initiliseBondIndexToConnectedAtomIndeces();
		this.initialiseRingBondsBitArray();
		this.initialiseAtomAdjacencyList();
	}
	
	public double getMeanNodeDegree() {
		double meanNodeDegree = 0.0;
		for(int i = 0; i < this.atomIndexToConnectedAtomIndeces.size(); i++) {
			meanNodeDegree += this.atomIndexToConnectedAtomIndeces.get(i).length;
		}
		meanNodeDegree /= this.atomIndexToConnectedAtomIndeces.size();
		return meanNodeDegree;
	}

	public int getNumNodeDegreeOne() {
		int numDegreeOne = 0;
		for(int i = 0; i < this.atomIndexToConnectedAtomIndeces.size(); i++) {
			numDegreeOne += this.atomIndexToConnectedAtomIndeces.get(i).length == 1 ? 1 : 0;
		}
		return numDegreeOne;
	}
	
	/**
	 * 
	 */
	protected void initiliseAtomIndexToConnectedAtomIndeces() {
		this.atomIndexToConnectedAtomIndeces = new java.util.Vector<short[]>();
		for(int i = 0; i < this.getNonHydrogenAtomCount(); i++) {
			java.util.List<IAtom> connectedAtoms = this.precursorMolecule.getConnectedAtomsList(this.precursorMolecule.getAtom(i));
			short[] connectedAtomIndeces = new short[connectedAtoms.size()];
			for(int k = 0; k < connectedAtoms.size(); k++)
				connectedAtomIndeces[k] = (short)this.precursorMolecule.getAtomNumber(connectedAtoms.get(k));
			this.atomIndexToConnectedAtomIndeces.add(i, connectedAtomIndeces);
		}
	}
	
	/**
	 * 
	 */
	protected void initiliseBondIndexToConnectedAtomIndeces() {
		this.bondIndexToConnectedAtomIndeces = new short[this.getNonHydrogenBondCount()][2];
		
		for(int i = 0; i < this.getNonHydrogenBondCount(); i++) {
			this.bondIndexToConnectedAtomIndeces[i][0] = (short)this.precursorMolecule.getAtomNumber(this.precursorMolecule.getBond(i).getAtom(0));
			this.bondIndexToConnectedAtomIndeces[i][1] = (short)this.precursorMolecule.getAtomNumber(this.precursorMolecule.getBond(i).getAtom(1));
		}
	}

	public DefaultBitArrayFragment toFragment() {
		return new DefaultBitArrayFragment(this);
	}
	
	/**
	 * initialise indeces belonging to a ring in the precursor molecule
	 */
	protected void initialiseRingBondsBitArray() throws Exception {
		this.aromaticBonds = new BitArray(this.getNonHydrogenBondCount());
		AllRingsFinder allRingsFinder = new AllRingsFinder();
		IRingSet ringSet = allRingsFinder.findAllRings(this.precursorMolecule);
		this.initialiseRingBondToBelongingRingBondIndecesBitArrays(ringSet);
		if (ringSet.getAtomContainerCount() != 0) {
			Aromaticity arom = new Aromaticity(ElectronDonation.cdk(), Cycles.cdkAromaticSet());
			java.util.Set<IBond> aromaticBonds = arom.findBonds(this.precursorMolecule);
			java.util.Iterator<IBond> it = aromaticBonds.iterator();
			while(it.hasNext()) {
				IBond currentBond = it.next();
				this.aromaticBonds.set(this.precursorMolecule.getBondNumber(currentBond), true);
			}
		}
	}
	
	/**
	 * initialises ringBondToBelongingRingBondIndeces BitArray array
	 * fast and easy way to retrieve all bond indeces belonging to a ring including the bond at specified index of that array 
	 * 
	 * @param ringSet
	 */
	protected void initialiseRingBondToBelongingRingBondIndecesBitArrays(IRingSet ringSet) {
		this.ringBondToBelongingRingBondIndeces = new BitArray[this.precursorMolecule.getBondCount() + 1];
		for (int i = 0; i < this.ringBondToBelongingRingBondIndeces.length; i++)
			this.ringBondToBelongingRingBondIndeces[i] = new BitArray(
					this.precursorMolecule.getBondCount() + 1);

		for (int i = 0; i < ringSet.getAtomContainerCount(); i++) {
			int[] indexes = new int[ringSet.getAtomContainer(i).getBondCount()];
			for (int j = 0; j < ringSet.getAtomContainer(i).getBondCount(); j++) {
				indexes[j] = this.precursorMolecule.getBondNumber(ringSet
						.getAtomContainer(i).getBond(j));
			}
			for (int j = 0; j < indexes.length; j++)
				this.ringBondToBelongingRingBondIndeces[indexes[j]].setBits(indexes);
		}
	}
	
	/**
	 * initialise 1D atom adjacency list
	 */
	protected void initialiseAtomAdjacencyList() {
		this.atomAdjacencyList = new short[getIndex(this.getNonHydrogenAtomCount() - 2, this.getNonHydrogenAtomCount() - 1) + 1];
		for(int i = 0; i < this.getNonHydrogenAtomCount(); i++) {
			java.util.List<IAtom> connectedAtoms = this.precursorMolecule.getConnectedAtomsList(this.precursorMolecule.getAtom(i));
			for(int k = 0; k < connectedAtoms.size(); k++) {
				int atomNumber = this.precursorMolecule.getAtomNumber(connectedAtoms.get(k));
				int bondNumber = this.precursorMolecule.getBondNumber(this.precursorMolecule.getAtom(i), connectedAtoms.get(k));
				this.atomAdjacencyList[getIndex(i, atomNumber)] = (short)(bondNumber + 1);
			}
		}
	}
	
	/**
	 * 
	 * @param bondIndex
	 * @return
	 */
	public BitArray getBitArrayOfBondsBelongingtoRingLikeBondIndex(short bondIndex) {
		return this.ringBondToBelongingRingBondIndeces[bondIndex];
	}
	
	/**
	 * returns atom indeces that are connected by bond with bondIndex
	 * 
	 * @param bondIndex
	 * @return
	 */
	public short[] getConnectedAtomIndecesOfAtomIndex(short atomIndex) {
		return this.atomIndexToConnectedAtomIndeces.get(atomIndex);
	}

	
	public short[] getConnectedAtomIndecesOfBondIndex(short bondIndex) {
		return this.bondIndexToConnectedAtomIndeces[bondIndex];
	}
	
	/**
	 * returns bond index + 1
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public short getBondIndexFromAtomAdjacencyList(short x, short y) {
		return this.atomAdjacencyList[this.getIndex(x, y)];
	}
	
	/**
	 * convert 2D matrix coordinates to 1D adjacency list coordinate
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	private int getIndex(int a, int b) {
		int row = a;
		int col = b;
		if (a > b) {
			row = b;
			col = a;
		}
		return row * this.getNonHydrogenAtomCount() + col - ((row + 1) * (row + 2)) / 2;
	}
	
	/**
	 * return true if bond belongs to an aromatic ring
	 * 
	 * @param bondIndex
	 * @return
	 */
	public boolean isAromaticBond(short bondIndex) {
		return this.aromaticBonds.get(bondIndex);
	}
	
	/**
	 * get order of bond
	 * SINGLE, DOUBLE, TRIPLE
	 * 
	 * @param bondIndex
	 * @return
	 */
	public String getBondOrder(short bondIndex) {
		return this.precursorMolecule.getBond(bondIndex).getOrder().toString();
	}
	
	public String getBondAsString(short index) {
		String[] atoms = this.getBondAtomsAsString(index);
		return this.getSingleBond(atoms[0], atoms[1], this.getBondOrder(index), this.isAromaticBond(index)).toString();
	}
	
	public Bond getBond(short index) {
		String[] atoms = this.getBondAtomsAsString(index);
		return this.getSingleBond(atoms[0], atoms[1], this.getBondOrder(index), this.isAromaticBond(index));
	}
	
	/**
	 * 
	 * @param bondIndex
	 * @return
	 */
	public String[] getBondAtomsAsString(short bondIndex) {
		String[] bondAtomsAsString = {this.precursorMolecule.getBond(bondIndex).getAtom(0).getSymbol(), this.precursorMolecule.getBond(bondIndex).getAtom(1).getSymbol()};
		return bondAtomsAsString;
	}
	
	protected Bond getSingleBond(String symbol1, String symbol2, String type, boolean isAromaticBond) {
		char typeChar = '=';
		if (!isAromaticBond) {
			if (type.equals("SINGLE"))
				typeChar = '-';
			else if (type.equals("DOUBLE"))
				typeChar = '=';
			else if (type.equals("TRIPLE"))
				typeChar = '~';
			else typeChar = '-';
		}
		Bond bond = new Bond(symbol1, symbol2, typeChar);
		return bond;
	}
	
	public void printBondsAndAtomIndeces() {
		for(int i = 0; i < this.getNonHydrogenBondCount(); i++) {
			short[] atomIndeces = this.getConnectedAtomIndecesOfBondIndex((short)i);
			System.out.println("Bond " + (i+1) + " " + (atomIndeces[0]+1) + " " + (atomIndeces[1]+1));
		}
	}
}
