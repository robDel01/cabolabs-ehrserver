package com.cabolabs.archetypeimport org.openehr.am.archetype.Archetypeimport org.openehr.am.archetype.constraintmodel.*import ehr.clinical_documents.DataIndeximport org.codehaus.groovy.grails.commons.ApplicationHolder/* * DataIndex generator. */class ArchetypeIndexer {      /**    * Archetype nodes that are defined by the RM and may not have a nodeID / definition on the ontology of the archetype.   * rmTypeName => path => text   */   /* this will not be needed for creating level 2 index definitions that are created here.   def rmDefinedNodes = [     'COMPOSITION': [      '/category': 'Composition category' // FIXME I18N     ],   ]   */      /**    * TODO: hay paths que existen en el RM y no estan en el AM, son atributos    *       obligatorios. Crear la path depende del RM TYPE del nodo que se    *       esta procesando, ej. ACTION tiene un atributo time, entonces hay que crear la path a ese atributo    */   def rmAttributes = [     'COMPOSITION': [      'language',    // CodePhrase      'territory',   // CodePhrase      'category',    // DvCodedText      'composer'     // PartyProxy como consultar por atributos de PartyProxy ??? hay que crear indices ????    ],    'EVENT_CONTEXT': [      'start_time', // DvDateTime      'end_time',   // DvDateTime      'location',   // String      'setting',    // DvCodedText      'health_care_facility', // PartyIdentified      'participations' // List<Participation> como consultar esto ??? hay que crear indices ????    ],    // each entry subclass include attributes from ENTRY    'ADMIN_ENTRY': [      'language',      'encoding',      'subject',      'provider',      'other_participations',      'workflow_id'    ],    'OBSERVATION': [      'language',      'encoding',      'subject',      'provider',      'other_participations',      'workflow_id',      'protocol',      'guideline_id'    ],    'EVALUATION': [      'language',      'encoding',      'subject',      'provider',      'other_participations',      'workflow_id',      'protocol',      'guideline_id'    ],    'INSTRUCTION': [      'language',      'encoding',      'subject',      'provider',      'other_participations',      'workflow_id',      'protocol',      'guideline_id',      'narrative',      'expiry_time',      'wf_definition'    ],    'ACTION': [      'language',      'encoding',      'subject',              // PartyProxy      'provider',             // PartyProxy      'other_participations', // List<Participation> como consulto esto??? necesito crear indices de Participation ???      'workflow_id',          // ObjectRef      'guideline_id',         // ObjectRef      'time'                  // DvDateTime    ],    'ACTIVITY': [      'timing',               // DvParsable      'action_archetype_id'   // String    ],    'ISM_TRANSITION': [      'current_state',        // DvCodedText      'transition',           // DvCodedText      'careflow_step'         // DvCodedText    ],    'INSTRUCTION_DETAILS': [      'instruction_id',       // LocatableRef      'activity_id'           // String    ],    'HISTORY': [      'origin',               // DvDateTime      'period',               // DvDuration      'duration'              // DvDuration    ],    'EVENT': [ // this class is abstract, should not be present on flat archetype      'time'                  // DvDateTime    ],    'POINT_EVENT': [      'time'                  // DvDateTime    ],    'INTERVAL_EVENT': [      'width',                // DvDuration      'sample_count',         // Integer      'math_function'         // DvCodedText    ],    'ELEMENT': [      'null_flavour'          // DvCodedText    ],    'DvQuantity': [      'units',      'magnitude',      'precision'    ],    'DV_QUANTITY': [      'units',      'magnitude',      'precision'    ],    'DV_TEXT': [      'value'    ],    'DV_CODED_TEXT': [      'value',      'defining_code/code_string',      'defining_code/terminology_id'    ],    'DV_DATE_TIME': [      'value'    ]   ]      def archetype   def indexes = []      // elems?   // [archetypeId:'openEHR-EHR-COMPOSITION.encounter.v1', path:'/context/setting', rmTypeName:'DV_CODED_TEXT']   def dataIndexes = []   /*    * Atributos de COMPOSITION que se indexan en nivel 1:   *  - uid   *  - name   *  - category   *  - composer   *  - context (fechas, setting, etc)   *   * En nivel 2 se indexa content, y solo los nodos hoja: ELEMENT.value    */      def index(String archetypeId)   {      def manager = ArchetypeManager.getInstance()      this.archetype = manager.getArchetype(archetypeId)      if (!this.archetype) throw new Exception("Arquetipo $archetypeId no encontrado")      index(this.archetype)   }      def index (Archetype archetype)   {      if (!archetype) throw new Exception("Arquetipo vacio")            this.archetype = archetype             // CObject      def co      def nodeID      def indexPath      def text            this.archetype.physicalPaths().sort().each { path ->              co = this.archetype.node(path)              /*         println path +" "+ co.rmTypeName                  if (!co.getParent())           println " * Parent: empty" // Empty solo para /         else           println " * Parent: " + co.getParent().rmAttributeName +" "+ co.getParent().path()         */                // No procesa el nodo /         if (!co.getParent()) return                      // Indices de nivel 2 solo para ELEMENT.value         if (co.rmTypeName == "ELEMENT")         {            nodeID = co.nodeID                        if (!nodeID) throw new Exception("No tiene nodeID: ELEMENT indefinido")                     // node name            def locale = ApplicationHolder.application.config.app.l10n.locale            def term = this.archetype.ontology.termDefinition(locale, nodeID)            if (!term)            {               //println " + ERROR: termino no definido para el nodo "+ nodeID            }            else            {               //println " + Node name = "+ term.getText()            }                        // FIXME: JAVA REF IMPL los tipos del RM son DvQuantity en lugar de DV_QUANTITY            //println " ~ index "+ co.path() +"/value "+ this.archetype.node( co.path() +"/value" ).rmTypeName                        indexPath = co.path() +"/value"                        indexes << new DataIndex(archetypeId: this.archetype.archetypeId.value,                                     path: indexPath,                                     rmTypeName: this.archetype.node( indexPath ).rmTypeName, // type de ELEMENT.value ej. DvQuantity                                     name: term.getText())                        //println ""         }                  // TODO: index HISTORY.origin DV_DATE_TIME         // TODO: index POINT_EVENT.time ... and other attributes and INTERVAL_EVENT ...               /*         nodeID = co.nodeID                  // Puede no tener nodeID por ser unico hermano (todos los nodos hoja son asi y algunos intermedios tambien)         // o pueden ser atributos que tienen su definicion en el RM y no estan descriptos en la ontologia del arquetipo.         if (!nodeID)         {            // El atributo esta definido en el RM?            // NO NECESITO indices para atributos que estan fijos en el RM aunque aparezcan en el arquetipo, van a estar            // mapeados a columnas fijas y puedo hacer que la query consulte derecho sobre esos.                        if ( rmDefinedNodes[co.rmTypeName] && rmDefinedNodes[co.rmTypeName][co.path()] )            {               println " = definicion en RM: "+ rmDefinedNodes[co.rmTypeName][co.path()] // FIXME: I18N                                             return // el texto es el de arriba, no hay que buscar en la ontologia            }            else            {               // FIXME BUG en parentNodePath() para /category, parentNodePath es / y da ''               def parentNodePath = co.getParent().parentNodePath()               if (!parentNodePath) parentNodePath = '/'                           // Pide la definicion del nodo a su abuelo (co -(padre)> ca -(abuelo)> co )               nodeID = this.archetype.node( parentNodePath ).nodeID                              println " = definicion en abuelo"            }         }         else         {            println " = definicion en el propio nodo"         }         */       /*       // Existen atributos en los arquetipos que tienen una definicion definida por el RM y su definicion no aparere en la ontologia del arquetipo.       // Entonces el texto no se puede sacar del arquetipo, popr lo que hay que tenerlo como metadatos.       // Ej. COMPOSITION.category (quiero crear indices por category, sino quisiera no me interesa su texto)       def nodeID = co.nodeID       if (!nodeID)       {          //println this.archetype.archetypeId.rmEntity // nombre del tipo del RM del arquetipo                 // co.parent es ca, ca tiene parentNodePath que es la path a su cco padre          //nodeID = this.archetype.node( co.getParent().parentNodePath() ).nodeID                  println "No tiene nodeID se pone el del abuelo "                  if ( co.getParent() )         {            println co.rmTypeName                     // FIXME BUG en parentNodePath() para /category, parentNodePath es / y da ''            def parentNodePath = co.getParent().parentNodePath()            if (!parentNodePath) parentNodePath = '/'                        println "tiene parent: co.parent.parentNodePath '"+ parentNodePath + "'"                        // co.parent es ca, ca tiene parentNodePath que es la path a su cco padre            nodeID = this.archetype.node( parentNodePath ).nodeID                        if (!nodeID)            {               throw new Exception("error 1")            }         }         else         {            throw new Exception("error 2")         }        }*/                       /* RM attributes (not in the archetype) for the current archetype node         this.rmAttributes[this.archetype.node(path).rmTypeName].each { attr ->           println " - "+ attr         }         */             } // physical paths            /*      this.archetype.getPathNodeMap().each { path, cobj ->         println path +" => "+ cobj.rmTypeName      }      */                  indexes.each { di ->                  if (!di.save())         {            println "======================"            println di.errors            println "======================"         }      }                // recursiva      //indexConstraintObject(this.archetype.definition)   }   private void indexConstraintObject(CComplexObject co)   {      if (!co.attributes || co.attributes.size()==0)      {         println "CCOe: "+ co.path() +" "+ co.rmTypeName +" "+ this.rmAttributes[co.rmTypeName]      }      else      {         println "CCOf: "+ co.path() +" "+ co.rmTypeName +" "+ this.rmAttributes[co.rmTypeName]         co.attributes.each { ca ->            indexConstraintAttribute(ca)         }      }   }   private void indexConstraintObject(CDomainType co)   {      /* CDomainType es abstracta      puede ser CCodePhrase, CDvOrdinal, CDvQuantity, CDvState      co.attirbutes.each { ca ->        indexConstraintAttribute(ca)      }      */      println "CDT: "+ co.path() +" "+ co.rmTypeName +" "+ co.class.simpleName +" "+ co.nodeID          // El tipo de dominio no tiene nodeID porque corresponde a ELEMENT.value     // Deberia pedir el nodeID del padre, pero el arquetipo tiene los parent == null     // TODO: clonar el adl parser y corregir lo de los parents.          def locale = ApplicationHolder.application.config.app.l10n.locale     def term = this.archetype.ontology.termDefinition(locale, co.nodeID)     if (!term)     {        println "ERROR: termino no definido para el nodo "+ co.nodeID     }          //indexes << new DataIndex(archetypeId: this.archetype.archetypeId.value, path: co.path(), rmTypeName: co.rmTypeName, name: term.getText())   }   private void indexConstraintObject(CPrimitiveObject co)   {      //solo tiene .item CPrimitive     println "CPR: "+ co.path() +" "+ co.rmTypeName   }   private void indexConstraintObject(CReferenceObject co)   {      throw new Exception("Reference found: only flat archetypes allowed "+ this.archetype.archetypeId.value +" "+ co.path())   }         private void indexConstraintAttribute(CAttribute ca)   {      if (!ca.children || ca.children.size()==0)      {         println "CAT: no tiene children "+ ca.path()      }      ca.children.each { co ->         indexConstraintObject(co)      }   }}