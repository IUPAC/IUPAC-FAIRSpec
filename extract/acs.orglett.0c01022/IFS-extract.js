{"IFS-extract-version":"0.1.0-alpha","pathway":[
 {"hash":"0c01022"},
 {"pubid":"acs.orglett.{hash}"},
 {"src":"IFS.finding.aid.source.publication.uri::https://doi.org/10.1021/{pubid}"},
 {"data":"IFS.finding.aid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/{pubid}/suppl_file/jo{hash}_si_002.zip"},

 {"path":"IFS.finding.aid.object::{data}|NMR DATA"},
 {"objects":"{path}/product/{IFS.nmr.representation.vender.dataset::{id=IFS.structure.param.compound.id::*}-{IFS.nmr.param.expt::*}.mnova}"},
 {"objects":"{path}/starting material/{IFS.nmr.representation.vender.dataset::{id=IFS.structure.param.compound.id::*}-{IFS.nmr.param.expt::*}.mnova}"}
]}