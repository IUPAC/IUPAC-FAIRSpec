package com.integratedgraphics.ifd.vendor.jcamp;

import java.util.Map;

import org.iupac.fairdata.extract.MetadataReceiverI;

import com.integratedgraphics.ifd.vendor.NMRVendorPlugin;
import com.integratedgraphics.ifd.vendor.jcamp.JCAMPDXIFDVendorPlugin.JCAMPPlugin;

public class JCAMPDXNMRPlugin extends NMRVendorPlugin implements JCAMPPlugin {

	protected final static String IFD_REP_DATAOBJECT_FAIRSPEC_NMR_VENDOR_DATASET = getProp("IFD_REP_DATAOBJECT_FAIRSPEC_NMR.VENDOR_DATASET");
    protected final static String IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_INSTR_MANUFACTURER_NAME = getProp("IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR.INSTR_MANUFACTURER_NAME");
	
//			##TITLE=diethyl phthalate
//			##JCAMPDX=5.00
//			##DATATYPE=NMR SPECTRUM
//			##DATACLASS=XYDATA
//			##ORIGIN=Department of Chemistry, UWI, ...
//			##OWNER=public domain
//			##LONGDATE=2012/03/19 16:44:19.0682 -0500
//			##.OBSERVEFREQUENCY=125.773
//			##.ACQUISITIONTIME=1.042
//			##.ZEROFILL=0
//			##$.SHIFTREFERENCE=INTERNAL,  ,1, -4.9982
//			##.OBSERVENUCLEUS=^13C
//			##.ACQUISITIONMODE=SIMULTANEOUS
//			##SPECTROMETERDATASYSTEM=Bruker Avance 500 MHz
//			##SOLVENTNAME=CHLOROFORM-D
//			##XUNITS=HZ
//			##YUNITS=ARBITRARY UNITS
//			##XFACTOR=1.0000000000
//			##YFACTOR=1.3874604702
//			##FIRSTX=25154.27929688
//			##LASTX=-628.64257813
//			##FIRSTY=-15.25636482
//			##DELTAX=-0.95950735
//			##NPOINTS=26872
//			##XYDATA=<data>

	static {
		register(com.integratedgraphics.ifd.vendor.jcamp.JCAMPDXNMRPlugin.class);
	}

    Map<String, String> map;

    
	public JCAMPDXNMRPlugin() {
	}
	
	@Override
	public void setMap(Map<String, String> map) {
		this.map = map;
		String date = map.get("##$DATE");
		if (date != null) {
			dataObjectLongID = Long.parseLong(date);
		}
	}

	@Override
	public String accept(MetadataReceiverI extractor, String originPath, byte[] bytes) {
		super.accept(extractor, originPath, bytes);
		// TODO
		return getVendorDataSetKey();
	}


	@Override
	public String getVendorName() {
		return "JCAMP-DX(NMR)";
	}

	@Override
	public String getVendorDataSetKey() {
		return IFD_REP_DATAOBJECT_FAIRSPEC_NMR_VENDOR_DATASET;
	}

    @Override
	public void reportVendor() {
		addProperty(IFD_PROPERTY_DATAOBJECT_FAIRSPEC_NMR_INSTR_MANUFACTURER_NAME, getVendorName());
	}
}

// Bruker:

//##TITLE=Cysteine base reaction T=22hrs...
//##JCAMPDX=6.0
//##DATATYPE=NMR SPECTRUM
//##DATACLASS=NTUPLES
//##ORIGIN=Bruker Analytik GmbH
//##OWNER=nmrsu
//##.OBSERVEFREQUENCY=400.132470966543
//##.OBSERVENUCLEUS=^1H
//##.ACQUISITIONMODE=SIMULTANEOUS (DQD)
//##.ACQUISITIONSCHEME=NOT PHASE SENSITIVE
//##.AVERAGES=16
//##.DIGITISERRES=18
//##SPECTROMETERDATASYSTEM=spect
//##.PULSESEQUENCE=zg30
//##.SOLVENTNAME=D2O
//##.SHIFTREFERENCE=(INTERNAL, D2O, 0, 16.1882)
//##$DU=<g:/>
//##$EXPNO=1820510
//##$NAME=<Apr26-2014>
//##$PROCNO=1
//##$TYPE=<nmr>
//##$USER=<research-beussmad>
//##$AMP=(0..31)
//100 100 100 100 100 10...
//##$AQSEQ=0
//##$AQMOD=3
//##$AUNM=<au_zg>
//##$AUTOPOS=<26 >
//##$BF1=400.13
//##$BF2=400.13
//##$BF3=400.13
//##$BF4=400.13
//##$BF5=500.13
//##$BF6=500.13
//##$BF7=500.13
//##$BF8=500.13
//##$BYTORDA=1
//##$CFDGTYP=0
//##$CFRGTYP=0
//##$CNST=(0..31)
//1 1 145 1 1 1 1 1 1 1 ...
//##$CPDPRG=<>
//##$CPDPRG1=<>
//##$CPDPRG2=<>
//##$CPDPRG3=<>
//##$CPDPRG4=<mlev>
//##$CPDPRG5=<mlev>
//##$CPDPRG6=<mlev>
//##$CPDPRG7=<mlev>
//##$CPDPRG8=<mlev>
//##$CPDPRGB=<>
//##$CPDPRGT=<>
//##$D=(0..31)
//0 1 0 0 0 0 0 0 0 0.06...
//##$DATE=1398538938
//##$DBL=(0..7)
//120 120 120 120 120 120...
//##$DBP=(0..7)
//150 150 150 150 150 150...
//##$DBP07=0
//##$DBPNAM0=<>
//##$DBPNAM1=<>
//##$DBPNAM2=<>
//##$DBPNAM3=<>
//##$DBPNAM4=<>
//##$DBPNAM5=<>
//##$DBPNAM6=<>
//##$DBPNAM7=<>
//##$DBPOAL=(0..7)
//0.5 0.5 0.5 0.5 0.5 0.5...
//##$DBPOFFS=(0..7)
//0 0 0 0 0 0 0 0
//##$DE=6
//##$DECBNUC=<off>
//##$DECIM=16
//##$DECNUC=<off>
//##$DECSTAT=4
//##$DIGMOD=1
//##$DIGTYP=7
//##$DL=(0..7)
//0 120 120 120 120 120 1...
//##$DP=(0..7)
//150 150 150 150 150 150...
//##$DP07=0
//##$DPNAME0=<>
//##$DPNAME1=<>
//##$DPNAME2=<>
//##$DPNAME3=<>
//##$DPNAME4=<>
//##$DPNAME5=<>
//##$DPNAME6=<>
//##$DPNAME7=<>
//##$DPOAL=(0..7)
//0.5 0.5 0.5 0.5 0.5 0.5...
//##$DPOFFS=(0..7)
//0 0 0 0 0 0 0 0
//##$DQDMODE=0
//##$DR=18
//##$DS=2
//##$DSLIST=<SSSSSSSSSSSSSSS>
//##$DSPFIRM=0
//##$DSPFVS=12
//##$DTYPA=0
//##$EXP=<PROTON>
//##$F1LIST=<111111111111111>
//##$F2LIST=<222222222222222>
//##$F3LIST=<333333333333333>
//##$FCUCHAN=(0..9)
//0 2 0 0 0 0 0 0 0 0
//##$FL1=90
//##$FL2=90
//##$FL3=90
//##$FL4=90
//##$FOV=20
//##$FQ1LIST=<freqlist>
//##$FQ2LIST=<freqlist>
//##$FQ3LIST=<freqlist>
//##$FQ4LIST=<freqlist>
//##$FQ5LIST=<freqlist>
//##$FQ6LIST=<freqlist>
//##$FQ7LIST=<freqlist>
//##$FQ8LIST=<freqlist>
//##$FS=(0..7)
//83 83 83 83 83 83 83 83
//##$FTLPGN=0
//##$FW=90000
//##$FNMODE=0
//##$GP031=0
//##$GPNAM0=<sine.100>
//##$GPNAM1=<sine.100>
//##$GPNAM10=<sine.100>
//##$GPNAM11=<sine.100>
//##$GPNAM12=<sine.100>
//##$GPNAM13=<sine.100>
//##$GPNAM14=<sine.100>
//##$GPNAM15=<sine.100>
//##$GPNAM16=<sine.100>
//##$GPNAM17=<sine.100>
//##$GPNAM18=<sine.100>
//##$GPNAM19=<sine.100>
//##$GPNAM2=<sine.100>
//##$GPNAM20=<sine.100>
//##$GPNAM21=<sine.100>
//##$GPNAM22=<sine.100>
//##$GPNAM23=<sine.100>
//##$GPNAM24=<sine.100>
//##$GPNAM25=<sine.100>
//##$GPNAM26=<sine.100>
//##$GPNAM27=<sine.100>
//##$GPNAM28=<sine.100>
//##$GPNAM29=<sine.100>
//##$GPNAM3=<sine.100>
//##$GPNAM30=<sine.100>
//##$GPNAM31=<sine.100>
//##$GPNAM4=<sine.100>
//##$GPNAM5=<sine.100>
//##$GPNAM6=<sine.100>
//##$GPNAM7=<sine.100>
//##$GPNAM8=<sine.100>
//##$GPNAM9=<sine.100>
//##$GPX=(0..31)
//0 0 0 0 0 0 0 0 0 0 0 ...
//##$GPY=(0..31)
//0 0 0 0 0 0 0 0 0 0 0 ...
//##$GPZ=(0..31)
//0 0 0 0 0 0 0 0 0 0 0 ...
//##$GRDPROG=<>
//##$HDDUTY=20
//##$HDRATE=20
//##$HGAIN=(0..3)
//0 0 0 0
//##$HL1=1
//##$HL2=35
//##$HL3=16
//##$HL4=17
//##$HOLDER=0
//##$HPMOD=(0..7)
//0 0 0 0 0 0 0 0
//##$HPPRGN=0
//##$IN=(0..31)
//0.001 0.001 0.001 0.00...
//##$INP=(0..31)
//0 0 0 0 0 0 0 0 0 0 0 ...
//##$INSTRUM=<spect>
//##$L=(0..31)
//1 1 1 1 1 1 1 1 1 1 1 ...
//##$LFILTER=200
//##$LGAIN=-5
//##$LOCKED=no
//##$LOCKPOW=-20
//##$LOCNUC=<2H>
//##$LOCPHAS=70
//##$LOCSHFT=yes
//##$LTIME=0.200000002980232
//##$MASR=4200
//##$MASRLST=<masrlst>
//##$NBL=1
//##$NC=-2
//##$NS=16
//##$NUC1=<1H>
//##$NUC2=<off>
//##$NUC3=<off>
//##$NUC4=<off>
//##$NUC5=<off>
//##$NUC6=<off>
//##$NUC7=<off>
//##$NUC8=<off>
//##$NUCLEI=0
//##$NUCLEUS=<off>
//##$O1=2470.96654339884
//##$O2=2470.96654339884
//##$O3=2470.96654339884
//##$O4=2470.96654339884
//##$O5=0
//##$O6=0
//##$O7=0
//##$O8=0
//##$OBSCHAN=(0..9)
//0 0 0 0 0 0 0 0 0 0
//##$OVERFLW=0
//##$P=(0..31)
//11.75 11.75 23.5 11.75...
//##$PAPS=2
//##$PARMODE=0
//##$PCPD=(0..9)
//100 90 90 100 100 100 1...
//##$PHCOR=(0..31)
//0 0 0 0 0 0 0 0 0 0 0 ...
//##$PHP=1
//##$PHREF=0
//##$PL=(0..31)
//120 -4 -4 120 120 120 ...
//##$POWMOD=0
//##$PR=1
//##$PRECHAN=(0..15)
//0 3 2 0 0 0 0 0 0 0 0 ...
//##$PRGAIN=0
//##$PROBHD=<5 mm BBO 1H/13C-15N/31P Z-GRD...
//##$PROSOL=no
//##$PULPROG=<zg30>
//##$PW=0
//##$QNP=1
//##$RD=0
//##$RECCHAN=(0..15)
//-1 -1 -1 -1 -1 -1 -1 -...
//##$RECPH=0
//##$RG=12.7
//##$RO=20
//##$ROUTWD1=(0..23)
//0 0 0 0 0 0 0 0 0 0 0 ...
//##$ROUTWD2=(0..23)
//0 0 0 0 0 1 0 0 0 0 0 ...
//##$RPUUSED=(0..8)
//0 0 0 0 0 0 0 0 0
//##$RSEL=(0..9)
//0 0 2 0 0 0 0 0 0 0
//##$S=(0..7)
//83 4 83 83 83 83 83 83
//##$SEOUT=0
//##$SFO1=400.132470966543
//##$SFO2=400.132470966543
//##$SFO3=400.132470966543
//##$SFO4=400.132470966543
//##$SFO5=500.13
//##$SFO6=500.13
//##$SFO7=500.13
//##$SFO8=500.13
//##$SOLVENT=<D2O>
//##$SP=(0..31)
//1 64.95 64.95 120 0 0 ...
//##$SP07=0
//##$SPNAM0=<gauss>
//##$SPNAM1=<Gaus1.1000>
//##$SPNAM10=<gauss>
//##$SPNAM11=<gauss>
//##$SPNAM12=<gauss>
//##$SPNAM13=<gauss>
//##$SPNAM14=<gauss>
//##$SPNAM15=<gauss>
//##$SPNAM16=<gauss>
//##$SPNAM17=<gauss>
//##$SPNAM18=<gauss>
//##$SPNAM19=<gauss>
//##$SPNAM2=<Gaus1.1000>
//##$SPNAM20=<gauss>
//##$SPNAM21=<gauss>
//##$SPNAM22=<gauss>
//##$SPNAM23=<gauss>
//##$SPNAM24=<gauss>
//##$SPNAM25=<gauss>
//##$SPNAM26=<gauss>
//##$SPNAM27=<gauss>
//##$SPNAM28=<gauss>
//##$SPNAM29=<gauss>
//##$SPNAM3=<Gaus1.1000>
//##$SPNAM30=<gauss>
//##$SPNAM31=<gauss>
//##$SPNAM4=<gauss>
//##$SPNAM5=<gauss>
//##$SPNAM6=<Squa100.1000>
//##$SPNAM7=<Gaus1.1000>
//##$SPNAM8=<gauss>
//##$SPNAM9=<gauss>
//##$SPOAL=(0..31)
//0.5 0.5 0.5 0.5 0.5 0....
//##$SPOFFS=(0..31)
//0 0 0 0 0 0 0 0 0 0 0 ...
//##$SW=20.0254193154209
//##$SWIBOX=(0..15)
//0 1 2 0 0 0 0 0 0 0 0 ...
//##$SWH=8012.82051282051
//##$TD=65536
//##$TD0=1
//##$TE=300
//##$TE2=300
//##$TL=(0..7)
//0 120 120 120 120 120 1...
//##$TP=(0..7)
//150 150 150 150 150 150...
//##$TP07=0
//##$TPNAME0=<>
//##$TPNAME1=<>
//##$TPNAME2=<>
//##$TPNAME3=<>
//##$TPNAME4=<>
//##$TPNAME5=<>
//##$TPNAME6=<>
//##$TPNAME7=<>
//##$TPOAL=(0..7)
//0.5 0.5 0.5 0.5 0.5 0.5...
//##$TPOFFS=(0..7)
//0 0 0 0 0 0 0 0
//##$TUNHIN=0
//##$TUNHOUT=0
//##$TUNXOUT=0
//##$USERA1=<user>
//##$USERA2=<user>
//##$USERA3=<user>
//##$USERA4=<user>
//##$USERA5=<user>
//##$V9=5
//##$VALIST=<valist>
//##$VCLIST=<CCCCCCCCCCCCCCC>
//##$VD=0
//##$VDLIST=<DDDDDDDDDDDDDDD>
//##$VPLIST=<PPPPPPPPPPPPPPP>
//##$VTLIST=<TTTTTTTTTTTTTTT>
//##$WBST=1024
//##$WBSW=4
//##$XGAIN=(0..3)
//0 0 0 0
//##$XL=0
//##$YL=0
//##$YMAXA=0
//##$YMINA=0
//##$ZGOPTNS=<>
//##$ZL1=120
//##$ZL2=120
//##$ZL3=120
//##$ZL4=120
//##$ABSF1=16.18818
//##$ABSF2=-3.837362
//##$ABSG=5
//##$ABSL=3
//##$ALPHA=0
//##$AQORDER=0
//##$ASSFAC=0
//##$ASSFACI=0
//##$ASSFACX=0
//##$ASSWID=0
//##$AUNMP=<proc_1d>
//##$AZFE=0.1
//##$AZFW=0.1
//##$BCFW=0
//##$BCMOD=0
//##$BYTORDP=0
//##$COROFFS=0
//##$CY=12.5
//##$DATMOD=1
//##$DC=0
//##$DFILT=<>
//##$DTYPP=0
//##$F1P=11.0000006437302
//##$F2P=-0.999999642372122
//##$FCOR=0.5
//##$FTSIZE=32768
//##$FTMOD=6
//##$GAMMA=0
//##$GB=0
//##$INTBC=1
//##$INTSCL=6.640681e-08
//##$ISEN=128
//##$LB=0.3
//##$LEV0=0
//##$LPBIN=0
//##$MAXI=10000
//##$MC2=0
//##$MEAN=0
//##$MEMOD=0
//##$MI=0
//##$NCOEF=0
//##$NCPROC=-2
//##$NLEV=6
//##$NOISF1=0
//##$NOISF2=0
//##$NSP=0
//##$NTHPI=0
//##$NZP=0
//##$OFFSET=16.18818
//##$PC=1
//##$PHC0=-26.50054
//##$PHC1=49.28028
//##$PHMOD=1
//##$PKNL=yes
//##$PPARMOD=0
//##$PSCAL=4
//##$PSIGN=0
//##$REVERSE=no
//##$SF=400.13
//##$SI=32768
//##$SIGF1=0
//##$SIGF2=0
//##$SINO=400
//##$SIOLD=32768
//##$SREGLST=<1H.D2O>
//##$SSB=0
//##$STSI=32768
//##$STSR=0
//##$SWP=8012.8205128205
//##$SYMM=0
//##$SDEV=0
//##$TDEFF=65536
//##$TDOFF=0
//##$TI=<>
//##$TILT=no
//##$TM1=0
//##$TM2=0
//##$TOPLEV=0
//##$USERP1=<user>
//##$USERP2=<user>
//##$USERP3=<user>
//##$USERP4=<user>
//##$USERP5=<user>
//##$WDW=1
//##$XDIM=32768
//##$YMAXP=396281523
//##$YMINP=-3944
//$$ End of Bruker specifi...
//##NTUPLES=NMR SPECTRUM
//##VARNAME=FREQUENCY,     SPECTRUM/REAL, ...
//##SYMBOL=X,             R,             ...
//##VARTYPE=INDEPENDENT,   DEPENDENT,     ...
//##VARFORM=AFFN,          ASDF,          ...
//##VARDIM=32768,         32768,         ...
//##UNITS=HZ,            ARBITRARY UNITS...
//##FACTOR=0.244539338749977, 1,         ...
//##FIRST=6477.37683290482, 0,          ...
//##LAST=-1535.44367991568, 0,         ...
//##MIN=-1535.44367991568, 0,         ...
//##MAX=6477.37683290482, 0,          ...
//##PAGE=N=1
//##DATATABLE=<data>


// MNova:

//##TITLE=Jul23-2022.41.fid
//##JCAMPDX=6.0
//##DATATYPE=MNOVA NMR VIEW DATA
//##BLOCKID=2
//##BLOCKS=2
//##ORIGIN=Mestrelab Research S.L.
//##OWNER=hrzepa
//##$MNOVALINKBLOCKTYPE=ITEM	$$ NMR Spectrum
//##$MNOVAITEMPOLYGON=(XY..XY)
//5321,5835
//8722,5835
//8...
//##$MNOVAITEMZ=0
//##$MNOVAITEMANGLE=0
//##DATACLASS=MNOVA VIEW DATA
//##$MNOVAITEMZOOMX=-20.698776542268014111,	240.66...
//##$MNOVAITEMZOOMY=-211.30368423461914063,	2192.8...

