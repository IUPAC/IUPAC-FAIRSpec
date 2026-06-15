The schema in this dist/ directory should
match the public schema of record in docs/schema,
which is the target for the schema's $id value.

(This field would not have to be an actual URL, but
we intend it to be.) 

It is created by 

    from

    src/main/resources/org/iupac/fairdata/contrib/fairspec/schema/fairspecSchemaTemplate.json

    as
    
    src/main/resources/org/iupac/fairdata/contrib/fairspec/schema/IFD.findingaid.schema.json
    
referencing


  "$schema": "http://json-schema.org/draft/2020-12/schema#",
  "$id": "https://iupac.github.io/IUPAC-FAIRSpec/schema/fairspec.schema.0.1.2.json",
