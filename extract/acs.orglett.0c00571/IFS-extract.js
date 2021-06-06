{"IFS-extract-version":"0.1.0-alpha","pathway":[
 {"hash":"0c00571"},
 {"pubid":"acs.orglett.{hash}"},
 {"src":"IFS.finding.aid.source.publication.uri::https://doi.org/10.1021/{pubid}"},
 {"data":"IFS.finding.aid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/{pubid}/suppl_file/ol{hash}_si_002.zip"},

 {"path":"IFS.finding.aid.object::{data}|FID for Publication/{id=IFS.structure.param.compound.id::*}.zip|{id}"},
 {"objects":"{path}/{IFS.structure.representation.mol.2d::{id}.mol}"},
 {"objects":"{path}/{IFS.nmr.representation.vender.dataset::{IFS.nmr.param.expt::*}-NMR.zip}"},
 {"objects":"{path}/HRMS.zip|{IFS.ms.representation.pdf::**/*.pdf}"},
]}
