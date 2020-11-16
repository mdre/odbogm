script sql

/*
let exist = select from (select expand(classes) from metadata:schema) where name = 'EdgeAttrib';
if ($exist.size() > 0) {
    delete edge EdgeAttrib;
    drop class EdgeAttrib;
}
*/
/*
let exist = select from (select expand(classes) from metadata:schema) where name = 'IndirectObject';
if ($exist.size() > 0) {
    delete vertex IndirectObject;
    drop class IndirectObject;
}
*/
/*
let exist = select from (select expand(classes) from metadata:schema) where name = 'UserSID';
if ($exist.size() > 0) {
    delete vertex UserSID;
    drop class UserSID;
}
*/
/*
let exist = select from (select expand(classes) from metadata:schema) where name = 'GroupSID';
if ($exist.size() > 0) {
    delete vertex GroupSID;
    drop class GroupSID;
}
*/
/*
let exist = select from (select expand(classes) from metadata:schema) where name = 'SObject';
if ($exist.size() > 0) {
    delete vertex SObject;
    drop class SObject;
}
*/
/*
let exist = select from (select expand(classes) from metadata:schema) where name = 'SSimpleVertex';
if ($exist.size() > 0) {
    delete vertex SSimpleVertex;
    drop class SSimpleVertex;
}
*/
/*
let exist = select from (select expand(classes) from metadata:schema) where name = 'SimpleVertex';
if ($exist.size() > 0) {
    delete vertex SimpleVertex;
    drop class SimpleVertex;
}
*/
/*
let exist = select from (select expand(classes) from metadata:schema) where name = 'SimpleVertexInterfaceAttr';
if ($exist.size() > 0) {
    delete vertex SimpleVertexInterfaceAttr;
    drop class SimpleVertexInterfaceAttr;
}
*/
/*
let exist = select from (select expand(classes) from metadata:schema) where name = 'SimpleVertexWithEmbedded';
if ($exist.size() > 0) {
    delete vertex SimpleVertexWithEmbedded;
    drop class SimpleVertexWithEmbedded;
}
*/
/*
let exist = select from (select expand(classes) from metadata:schema) where name = 'SimpleVertexWithImplement';
if ($exist.size() > 0) {
    delete vertex SimpleVertexWithImplement;
    drop class SimpleVertexWithImplement;
}
*/
/*
let exist = select from (select expand(classes) from metadata:schema) where name = 'FooNode';
if ($exist.size() > 0) {
    delete vertex FooNode;
    drop class FooNode;
}
*/
/*
let exist = select from (select expand(classes) from metadata:schema) where name = 'Enums';
if ($exist.size() > 0) {
    delete vertex Enums;
    drop class Enums;
}
*/
/*
let exist = select from (select expand(classes) from metadata:schema) where name = 'SimpleVertexEx';
if ($exist.size() > 0) {
    delete vertex SimpleVertexEx;
    drop class SimpleVertexEx;
}
*/
/*
let exist = select from (select expand(classes) from metadata:schema) where name = 'SVExChild';
if ($exist.size() > 0) {
    delete vertex SVExChild;
    drop class SVExChild;
}
*/
let exist = select from (select expand(classes) from metadata:schema) where name = 'EdgeAttrib';
if ($exist.size() = 0) {
    create class EdgeAttrib extends E;
}
alter class EdgeAttrib custom javaClass='test.EdgeAttrib';


let exist = select from (select expand(properties)  from (select expand(classes)  from metadata:schema)  where name = 'EdgeAttrib') where name = 'uuid';
if ($exist.size()=0) {
    create property EdgeAttrib.uuid STRING;
}


let exist = select from (select expand(properties)  from (select expand(classes)  from metadata:schema)  where name = 'EdgeAttrib') where name = 'nota';
if ($exist.size()=0) {
    create property EdgeAttrib.nota STRING;
}
 

let exist = select from (select expand(properties)  from (select expand(classes)  from metadata:schema)  where name = 'EdgeAttrib') where name = 'fecha';
if ($exist.size()=0) {
    create property EdgeAttrib.fecha DATETIME;
}
 

let exist = select from (select expand(properties)  from (select expand(classes)  from metadata:schema)  where name = 'EdgeAttrib') where name = 'enumValue';
if ($exist.size()=0) {
    create property EdgeAttrib.enumValue string;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'IndirectObject';
if ($exist.size() = 0) {
    create class IndirectObject extends V;
}
alter class IndirectObject custom javaClass='test.IndirectObject';


let exist = select from (select expand(properties)  from (select expand(classes)  from metadata:schema)  where name = 'IndirectObject') where name = 'testData';
if ($exist.size()=0) {
    create property IndirectObject.testData STRING;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'IndirectObject_directLink';
if ($exist.size()=0) {
    create class IndirectObject_directLink extends E;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'IndirectObject_alDirectLinked';
if ($exist.size()=0) {
    create class IndirectObject_alDirectLinked extends E;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'IndirectObject_hmDirectLinked';
if ($exist.size()=0) {
    create class IndirectObject_hmDirectLinked extends E;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'IndirectObject_indirectLink';
if ($exist.size()=0) {
    create class IndirectObject_indirectLink extends E;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'IndirectObject_indirectLinkedFromAL';
if ($exist.size()=0) {
    create class IndirectObject_indirectLinkedFromAL extends E;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'IndirectObject_alIndirectLinked';
if ($exist.size()=0) {
    create class IndirectObject_alIndirectLinked extends E;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'IndirectObject_hmIndirectLinked';
if ($exist.size()=0) {
    create class IndirectObject_hmIndirectLinked extends E;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'UserSID';
if ($exist.size() = 0) {
    create class UserSID extends V;
}
alter class UserSID custom javaClass='net.odbogm.security.UserSID';


let exist = select from (select expand(properties)  from (select expand(classes)  from metadata:schema)  where name = 'UserSID') where name = 'name';
if ($exist.size()=0) {
    create property UserSID.name STRING;
}
 

let exist = select from(select expand(indexes) from metadata:indexmanager) where name = 'UserSID.name';
if ($exist.size()=0) {
    create index UserSID.name on UserSID(name) UNIQUE;
}
 

let exist = select from (select expand(properties)  from (select expand(classes)  from metadata:schema)  where name = 'UserSID') where name = 'uuid';
if ($exist.size()=0) {
    create property UserSID.uuid STRING;
}
 

let exist = select from(select expand(indexes) from metadata:indexmanager) where name = 'UserSID.uuid';
if ($exist.size()=0) {
    create index UserSID.uuid on UserSID(uuid) UNIQUE;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'UserSID_groups';
if ($exist.size()=0) {
    create class UserSID_groups extends E;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'GroupSID';
if ($exist.size() = 0) {
    create class GroupSID extends V;
}
alter class GroupSID custom javaClass='net.odbogm.security.GroupSID';


let exist = select from (select expand(properties)  from (select expand(classes)  from metadata:schema)  where name = 'GroupSID') where name = 'name';
if ($exist.size()=0) {
    create property GroupSID.name STRING;
}
 

let exist = select from(select expand(indexes) from metadata:indexmanager) where name = 'GroupSID.name';
if ($exist.size()=0) {
    create index GroupSID.name on GroupSID(name) UNIQUE;
}
 

let exist = select from (select expand(properties)  from (select expand(classes)  from metadata:schema)  where name = 'GroupSID') where name = 'uuid';
if ($exist.size()=0) {
    create property GroupSID.uuid STRING;
}
 

let exist = select from(select expand(indexes) from metadata:indexmanager) where name = 'GroupSID.uuid';
if ($exist.size()=0) {
    create index GroupSID.uuid on GroupSID(uuid) UNIQUE;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'GroupSID_participants';
if ($exist.size()=0) {
    create class GroupSID_participants extends E;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'GroupSID_addedTo';
if ($exist.size()=0) {
    create class GroupSID_addedTo extends E;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'SObject';
if ($exist.size() = 0) {
    create class SObject extends V;
}
alter class SObject custom javaClass='net.odbogm.security.SObject';


let exist = select from (select expand(classes) from metadata:schema) where name = 'SObject___owner';
if ($exist.size()=0) {
    create class SObject___owner extends E;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'SObject___acl';
if ($exist.size()=0) {
    create class SObject___acl extends E;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'SObject___inherit';
if ($exist.size()=0) {
    create class SObject___inherit extends E;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'SSimpleVertex';
if ($exist.size() = 0) {
    create class SSimpleVertex extends SObject;
}
alter class SSimpleVertex custom javaClass='test.SSimpleVertex';


let exist = select from (select expand(properties)  from (select expand(classes)  from metadata:schema)  where name = 'SSimpleVertex') where name = 's';
if ($exist.size()=0) {
    create property SSimpleVertex.s STRING;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'SSimpleVertex___owner';
if ($exist.size()=0) {
    create class SSimpleVertex___owner extends SObject___owner;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'SSimpleVertex___acl';
if ($exist.size()=0) {
    create class SSimpleVertex___acl extends SObject___acl;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'SSimpleVertex___inherit';
if ($exist.size()=0) {
    create class SSimpleVertex___inherit extends SObject___inherit;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'SimpleVertex';
if ($exist.size() = 0) {
    create class SimpleVertex extends V;
}
alter class SimpleVertex custom javaClass='test.SimpleVertex';


let exist = select from (select expand(properties)  from (select expand(classes)  from metadata:schema)  where name = 'SimpleVertex') where name = 'uuid';
if ($exist.size()=0) {
    create property SimpleVertex.uuid STRING;
}
 

let exist = select from (select expand(properties)  from (select expand(classes)  from metadata:schema)  where name = 'SimpleVertex') where name = 's';
if ($exist.size()=0) {
    create property SimpleVertex.s STRING;
}
 

let exist = select from (select expand(properties)  from (select expand(classes)  from metadata:schema)  where name = 'SimpleVertex') where name = 'i';
if ($exist.size()=0) {
    create property SimpleVertex.i INTEGER;
}
 

let exist = select from (select expand(properties)  from (select expand(classes)  from metadata:schema)  where name = 'SimpleVertex') where name = 'f';
if ($exist.size()=0) {
    create property SimpleVertex.f FLOAT;
}
 

let exist = select from (select expand(properties)  from (select expand(classes)  from metadata:schema)  where name = 'SimpleVertex') where name = 'b';
if ($exist.size()=0) {
    create property SimpleVertex.b BOOLEAN;
}
 

let exist = select from (select expand(properties)  from (select expand(classes)  from metadata:schema)  where name = 'SimpleVertex') where name = 'fecha';
if ($exist.size()=0) {
    create property SimpleVertex.fecha DATETIME;
}


let exist = select from (select expand(properties)  from (select expand(classes)  from metadata:schema)  where name = 'SimpleVertex') where name = 'serial';
if ($exist.size()=0) {
    create property SimpleVertex.serial LONG;
}
 

let exist = select from (select expand(properties)  from (select expand(classes)  from metadata:schema)  where name = 'SimpleVertex') where name = 'oI';
if ($exist.size()=0) {
    create property SimpleVertex.oI INTEGER;
}
 

let exist = select from (select expand(properties)  from (select expand(classes)  from metadata:schema)  where name = 'SimpleVertex') where name = 'oF';
if ($exist.size()=0) {
    create property SimpleVertex.oF FLOAT;
}
 

let exist = select from (select expand(properties)  from (select expand(classes)  from metadata:schema)  where name = 'SimpleVertex') where name = 'oB';
if ($exist.size()=0) {
    create property SimpleVertex.oB BOOLEAN;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'SimpleVertexInterfaceAttr';
if ($exist.size() = 0) {
    create class SimpleVertexInterfaceAttr extends SimpleVertex;
}
alter class SimpleVertexInterfaceAttr custom javaClass='test.SimpleVertexInterfaceAttr';


let exist = select from (select expand(classes) from metadata:schema) where name = 'SimpleVertexInterfaceAttr_itest';
if ($exist.size()=0) {
    create class SimpleVertexInterfaceAttr_itest extends E;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'SimpleVertexInterfaceAttr_iList';
if ($exist.size()=0) {
    create class SimpleVertexInterfaceAttr_iList extends E;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'SimpleVertexWithEmbedded';
if ($exist.size() = 0) {
    create class SimpleVertexWithEmbedded extends V;
}
alter class SimpleVertexWithEmbedded custom javaClass='test.SimpleVertexWithEmbedded';


let exist = select from (select expand(classes) from metadata:schema) where name = 'SimpleVertexWithEmbedded_stringlist';
if ($exist.size()=0) {
    create class SimpleVertexWithEmbedded_stringlist extends E;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'SimpleVertexWithEmbedded_simplemap';
if ($exist.size()=0) {
    create class SimpleVertexWithEmbedded_simplemap extends E;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'SimpleVertexWithImplement';
if ($exist.size() = 0) {
    create class SimpleVertexWithImplement extends SimpleVertex;
}
alter class SimpleVertexWithImplement custom javaClass='test.SimpleVertexWithImplement';


let exist = select from (select expand(classes) from metadata:schema) where name = 'FooNode';
if ($exist.size() = 0) {
    create class FooNode extends V;
}
alter class FooNode custom javaClass='test.Foo';


let exist = select from (select expand(properties)  from (select expand(classes)  from metadata:schema)  where name = 'FooNode') where name = 'text';
if ($exist.size()=0) {
    create property FooNode.text STRING;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'FooNode_lsve';
if ($exist.size()=0) {
    create class FooNode_lsve extends E;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'Enums';
if ($exist.size() = 0) {
    create class Enums extends V;
}
alter class Enums custom javaClass='test.Enums';


let exist = select from (select expand(properties)  from (select expand(classes)  from metadata:schema)  where name = 'Enums') where name = 'description';
if ($exist.size()=0) {
    create property Enums.description STRING;
}
 

let exist = select from (select expand(properties)  from (select expand(classes)  from metadata:schema)  where name = 'Enums') where name = 'theEnum';
if ($exist.size()=0) {
    create property Enums.theEnum string;
}
 

let exist = select from (select expand(properties)  from (select expand(classes)  from metadata:schema)  where name = 'Enums') where name = 'enums';
if ($exist.size()=0) {
    create property Enums.enums embeddedlist;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'Enums_enumToString';
if ($exist.size()=0) {
    create class Enums_enumToString extends E;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'Enums_stringToEnum';
if ($exist.size()=0) {
    create class Enums_stringToEnum extends E;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'SimpleVertexEx';
if ($exist.size() = 0) {
    create class SimpleVertexEx extends SimpleVertex;
}
alter class SimpleVertexEx custom javaClass='test.SimpleVertexEx';


let exist = select from (select expand(properties)  from (select expand(classes)  from metadata:schema)  where name = 'SimpleVertexEx') where name = 'svex';
if ($exist.size()=0) {
    create property SimpleVertexEx.svex STRING;
}
 
alter property SimpleVertexEx.svex mandatory TRUE;

let exist = select from (select expand(classes) from metadata:schema) where name = 'SimpleVertexEx_looptest';
if ($exist.size()=0) {
    create class SimpleVertexEx_looptest extends E;
}

let exist = select from (select expand(classes) from metadata:schema) where name = 'SimpleVertexEx_eagerTest';
if ($exist.size()=0) {
    create class SimpleVertexEx_eagerTest extends E;
}
 

let exist = select from (select expand(properties)  from (select expand(classes)  from metadata:schema)  where name = 'SimpleVertexEx') where name = 'enumTest';
if ($exist.size()=0) {
    create property SimpleVertexEx.enumTest string;
}
 

let exist = select from (select expand(properties)  from (select expand(classes)  from metadata:schema)  where name = 'SimpleVertexEx') where name = 'svuuid';
if ($exist.size()=0) {
    create property SimpleVertexEx.svuuid STRING;
}
 

let exist = select from(select expand(indexes) from metadata:indexmanager) where name = 'SimpleVertexEx.svuuid';
if ($exist.size()=0) {
    create index SimpleVertexEx.svuuid on SimpleVertexEx(svuuid) UNIQUE;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'SimpleVertexEx_svinner';
if ($exist.size()=0) {
    create class SimpleVertexEx_svinner extends E;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'SimpleVertexEx_alString';
if ($exist.size()=0) {
    create class SimpleVertexEx_alString extends E;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'SimpleVertexEx_hmString';
if ($exist.size()=0) {
    create class SimpleVertexEx_hmString extends E;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'SimpleVertexEx_alSV';
if ($exist.size()=0) {
    create class SimpleVertexEx_alSV extends E;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'SimpleVertexEx_lSV';
if ($exist.size()=0) {
    create class SimpleVertexEx_lSV extends E;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'SimpleVertexEx_alSVE';
if ($exist.size()=0) {
    create class SimpleVertexEx_alSVE extends E;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'SimpleVertexEx_hmSV';
if ($exist.size()=0) {
    create class SimpleVertexEx_hmSV extends E;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'SimpleVertexEx_hmSVE';
if ($exist.size()=0) {
    create class SimpleVertexEx_hmSVE extends E;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'SimpleVertexEx_ohmSVE';
if ($exist.size()=0) {
    create class SimpleVertexEx_ohmSVE extends EdgeAttrib;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'SVExChild';
if ($exist.size() = 0) {
    create class SVExChild extends SimpleVertexEx;
}
alter class SVExChild custom javaClass='test.SVExChild';


let exist = select from (select expand(classes) from metadata:schema) where name = 'SVExChild_looptest';
if ($exist.size()=0) {
    create class SVExChild_looptest extends SimpleVertexEx_looptest;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'SVExChild_svinner';
if ($exist.size()=0) {
    create class SVExChild_svinner extends SimpleVertexEx_svinner;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'SVExChild_alString';
if ($exist.size()=0) {
    create class SVExChild_alString extends SimpleVertexEx_alString;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'SVExChild_hmString';
if ($exist.size()=0) {
    create class SVExChild_hmString extends SimpleVertexEx_hmString;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'SVExChild_alSV';
if ($exist.size()=0) {
    create class SVExChild_alSV extends SimpleVertexEx_alSV;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'SVExChild_lSV';
if ($exist.size()=0) {
    create class SVExChild_lSV extends SimpleVertexEx_lSV;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'SVExChild_alSVE';
if ($exist.size()=0) {
    create class SVExChild_alSVE extends SimpleVertexEx_alSVE;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'SVExChild_hmSV';
if ($exist.size()=0) {
    create class SVExChild_hmSV extends SimpleVertexEx_hmSV;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'SVExChild_hmSVE';
if ($exist.size()=0) {
    create class SVExChild_hmSVE extends SimpleVertexEx_hmSVE;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'SVExChild_ohmSVE';
if ($exist.size()=0) {
    create class SVExChild_ohmSVE extends EdgeAttrib;
}


let exist = select from (select expand(classes) from metadata:schema) where name = 'Serial';
if ($exist.size() = 0) {
    create class Serial extends V;
}
alter class Serial custom javaClass='test.Serial';


let exist = select from (select expand(properties)  from (select expand(classes)  from metadata:schema)  where name = 'Serial') where name = 's1';
if ($exist.size()=0) {
    create property Serial.s1 LONG;
}


let exist = select from (select expand(properties)  from (select expand(classes)  from metadata:schema)  where name = 'Serial') where name = 's2';
if ($exist.size()=0) {
    create property Serial.s2 LONG;
}


create sequence test_sequence type ordered;


let exist = select from (select expand(classes) from metadata:schema) where name = 'Secure';
if ($exist.size() = 0) {
    create class Secure extends SObject;
}
alter class Secure custom javaClass='test.Secure';


let exist = select from (select expand(classes) from metadata:schema) where name = 'Secure_subs';
if ($exist.size()=0) {
    create class Secure_subs extends E;
}
 

let exist = select from (select expand(properties)  from (select expand(classes)  from metadata:schema)  where name = 'Secure') where name = 's';
if ($exist.size()=0) {
    create property Secure.s STRING;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'Secure___owner';
if ($exist.size()=0) {
    create class Secure___owner extends SObject___owner;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'Secure___inherit';
if ($exist.size()=0) {
    create class Secure___inherit extends SObject___inherit;
}
 

let exist = select from (select expand(classes) from metadata:schema) where name = 'SubSecure';
if ($exist.size() = 0) {
    create class SubSecure extends V;
}
alter class SubSecure custom javaClass='test.SubSecure';


let exist = select from (select expand(classes) from metadata:schema) where name = 'SubSecure_aList';
if ($exist.size()=0) {
    create class SubSecure_aList extends E;
}
 

end