package com.boswinner.bos.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.boswinner.bos.constant.BosCodeEnum;
import com.boswinner.bos.constant.BosConstants;
import com.boswinner.bos.entity.*;
import com.boswinner.bos.exception.BadRequestParameterException;
import com.boswinner.bos.exception.BosException;
import com.boswinner.bos.exception.FileSystemException;
import com.boswinner.bos.repository.*;
import com.boswinner.util.CsvDataUtil;
import com.boswinner.util.RoadMapJava;
import com.boswinner.vmodel.BosResponseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.joda.time.DateTime;
import org.json.simple.JSONObject;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletOutputStream;

@Component
public class ComponentService {
    protected org.slf4j.Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    ComponentRepository comRepo;

    @Autowired
    OutlineRepository outlineRepo;

    @Autowired
    @Lazy
    FileService fileService;

    @Autowired
    private FileRepository fileRepo;

    @Autowired
    PointRepository pointRepo;

    @Autowired
    RouteRepository routeRepo;
    @Autowired
    RouteService routeService;

    @Autowired
    BosModelListRepository bosModelListRepo;

    @Autowired
    EdgeRepository edgeRepo;

    @Autowired
    AttributeRepository attributeRepo;

    @Autowired
    BosGridsRepository bosGridsRepo;

    @Autowired
    BosStairGridRepository bosStairGridRepo;
    @Autowired
    BosSpaceTreeRepository bosSpaceTreeRepo;

    @Autowired
    BosUnitRepository bosUnitRepository;
    @Autowired
    private BosModelListRepository modelListRepository;
    @Autowired
    private AccountService accService;
    @Autowired
    @Lazy
    private IfcService ifcService;
    @Autowired
    private GLTFRepository gltfRepo;
    @Autowired
    private BosSystemGroupRepository bosSystemGroupRepo;
    @Autowired
    private BosHoseConnectRepository bosHoseConnectRepo;

    @Value("${grid_file_path}")
    private String gridFilePath;
    @Value("${arangodb.host}")
    private String arangodbHost;

    @Value("${arangodb.port}")
    private String arangodbPort;

    @Value("${arangodb.user}")
    private String arangodbUser;

    @Value("${arangodb.password}")
    private String arangodbPassword;
    @Value("${arangodb.db}")
    private String arangodbDB;

    @Autowired
    private FileComparisonRepository fileComparisonRepo;
    @Autowired
    private BosGridsRepository bosGridsRepository;

    private static final Logger LOGGER = LogManager.getLogger();

    private static int COLUMN_NUM;

    public Iterable<BosComponent> findBosComponentsByFile(String fileid) {
        return comRepo.findByModel("files/" + fileid);
    }

    public Iterable<BosComponent> findBosComponentsByFileAndLimit(String fileid, int pageIndex, int pageSize) {
        return comRepo.getByModelAndLimit("files/" + fileid, pageIndex, pageSize);
    }
    
    public Iterable<BosComponent> findBosComponentsByFilesAndType(String fileid) {
        return comRepo.findByModelAndType("files/" + fileid, "IFCPROJECT");
    }

    public Iterable<BosComponent> findBosComponentsByGeometry(String path) {
        return comRepo.findByGeometry(path);
    }

    public List<BosComponent> findBosComponentsByParent(String parent) {
        Iterable<BosComponent> iterCom = comRepo.findByParent(parent);

        List<BosComponent> lstCom = new ArrayList<BosComponent>();
        for (BosComponent com : iterCom) {
            lstCom.add(com);
        }

        return lstCom;
    }

    public Iterable<String> findModelTypes(String model) {
        if (!model.startsWith("files/")) {
            model = "files/" + model;
        }
        return comRepo.findModelTypes(model);
    }


    public Iterable<String> findComponentTypes(String component) {
        //if(!model.startsWith("files/")){
        //	model = "files/" + model;
        //}
        return comRepo.findComponentTypes(component);
    }

    public List<BosComponent> findBosComponentByType(String fileid, String type) {
        Iterable<BosComponent> iterCom = comRepo.findByModelAndType("files/" + fileid, type);
        List<BosComponent> lstCom = Lists.newArrayList(iterCom);
        return lstCom;
    }
    
    /**
     * 查找系统树（前端使用）
     * @param fileid
     * @param type
     * @return
     */
    public List<HashMap<String, Object>> findSystem(String fileid, String type, String name) {    
        List<HashMap<String, Object>> lstCom = Lists.newArrayList();

        Iterable<BosComponent> iterCom = null;
        if (StringUtils.isEmpty(name)) { // name为空时
        	iterCom =  comRepo.findByModelAndType("files/" + fileid, type);
        }else {
        	iterCom = queryComponentsByModelTypeAndName("files/" + fileid, type, name);
		}
        
        for (BosComponent com : iterCom) {
            Optional<BosSystemGroup> byKey = bosSystemGroupRepo.findByKey(com.getKey());
            BosComponentDTO bosComponentDTO = new BosComponentDTO();
            bosComponentDTO.setSystemgroup(byKey.isPresent()==true?byKey.get().getSystemgroup():null);
            try {
                BeanUtils.copyProperties(bosComponentDTO, com);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            HashMap<String, Object> map = new HashMap<String,Object>();
            map.put("system", bosComponentDTO);
            lstCom.add(map);
        }

        return lstCom;
    }

    public Optional<BosComponent> findBosComponentByKey(String componentid) {

        return comRepo.findByKey(componentid);
    }
    
    /**
     * 根据model/type/name查询构件
     * @param model
     * @param type
     * @param name
     * @return
     */
    public List<BosComponent> queryComponentsByModelTypeAndName(String model, String type, String name) {
        return Lists.newArrayList(comRepo.getWithModelTypeAndName(model, type, "%"+name+"%"));
    }
    
    @Async
    public Vector<String> getOutlineCom(String model, String vstxt, int level,IdentityHashMap<String, WebSocketServer> webSocketSet) {

        // 存到数据库
        BosOutline outline = new BosOutline();
        outline.setKey(model + "_" + level);
        outline.setLevel(level);
        outline.setModel(model);
        outline.setStatus("0.5");
        outlineRepo.save(outline);
           
        // 发送外轮廓提取状态实时消息
        BosFile bosFile = fileService.findByKey(model);
		String devcode = accService.getDevcodeByAccount(bosFile.getAccount());
		send(webSocketSet, devcode, model, "0.5",0,"",false,level,true,3);

        final double startTime = System.nanoTime();
        LOGGER.info(model + ":开始提取外轮廓");

        Vector<String> vsComOutline = new Vector<String>();
        try {
            OutlineService om = new OutlineService(level);
            om.setData(vstxt);
            om.getOutline(vsComOutline, level);
        } catch (Exception e) {
            outline.setStatus("2");
            outlineRepo.save(outline);
            send(webSocketSet, devcode, model, "2",0,"",false,level,true,3);
        }

        final double getOutlineEndTime = System.nanoTime();
        LOGGER.info(String.format(model + ":外轮廓提取完成！共耗时 %.2f 秒。",
                (getOutlineEndTime - startTime) / 1.E9));

        outline.setOutlines(vsComOutline);
        outline.setStatus("1");
        outlineRepo.save(outline);
        
        send(webSocketSet, devcode, model, "1",0,"",false,level,true,3);

        return vsComOutline;
    }

    private Map<Double, BosGrids> gridsMap = new HashMap<>();

    @Async
    public void sumModelList() {
        List<String> files = new ArrayList<>();
        Iterable<BosFile> bosFiles = fileRepo.findAll();
        for (BosFile bosFile : bosFiles) {
            if (bosFile.getStatus().equals("1")) {
                files.add(bosFile.getKey());
            }
        }
        List<String> models = new ArrayList<>();
        Iterable<BosModelList> bosModelLists = bosModelListRepo.findAll();
        for (BosModelList bosModelList : bosModelLists) {
            models.add(bosModelList.getKey());
        }
        files.removeAll(models);
        for (String modelkey : files) {
            List<String> componentslist = new ArrayList<>();
            Iterable<BosComponent> bosComponents = comRepo.findByModel("files/" + modelkey);
            for (BosComponent bosComponent : bosComponents) {
                componentslist.add(bosComponent.getKey());
            }
            BosModelList bosModelList = new BosModelList();
            bosModelList.setComponentlist(componentslist);
            bosModelList.setKey(modelkey);
            bosModelListRepo.save(bosModelList);
            LOGGER.info(modelkey + ":已获取索引");
        }
    }

    boolean sendFlag = false;

    @Async
    public void saveSameModel(String filekey, String samefilekey, IdentityHashMap<String, WebSocketServer> webSocketSet) {
        BosFile bosFile = fileService.getFile(filekey);
        BosFile samefile = fileService.getFile(samefilekey);
        samefile.setComponentCount(comRepo.countByModel(samefilekey));
        fileRepo.save(samefile);

        bosFile.setStatus("0.5");
        bosFile.setComponentCount(samefile.getComponentCount());
        fileRepo.save(bosFile);
        // 发送实时消息
        String devcode = accService.getDevcodeByAccount(bosFile.getAccount());
        send(webSocketSet, devcode, filekey, "0.5",-1,"0%",true,0,false,1);
        sendFlag = false;

        final double starTime = System.nanoTime();
        LOGGER.info(filekey + ":正在复用几何数据。");

        List<String> componentlist = copyComponent(filekey, samefilekey,webSocketSet,devcode);

        BosFile file = fileService.getFile(filekey);
        if (componentlist.size() != 0) {
            BosModelList bosModelList2 = new BosModelList();
            bosModelList2.setComponentlist(componentlist);
            bosModelList2.setKey(file.getKey());
            bosModelListRepo.save(bosModelList2);
            file.setStatus("1");

            // 发送实时消息
            send(webSocketSet, devcode, filekey, "1",-1,"100%",true,0,false,1);
            sendFlag = true;

            final double endTime = System.nanoTime();
            double writingTime = (endTime - starTime) / 1.E9;
            LOGGER.info(file + ":几何复用完毕，复用时间为:" + writingTime);
            fileRepo.save(file);
            try {
                saveSpaceTrees(file, false, null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            file.setStatus("2");
            LOGGER.info(":读取复用文件" + samefilekey + "异常，请尝试强制解析");
            fileRepo.save(file);
        }
    }

    public boolean isSendFlag() {
        return sendFlag;
    }

    /**
     * 发送websocket即时消息
     * @param webSocketSet
     * @param devcode
     * @param fileKey
     * @param status
     * @param position
     * @param time
     * @param flag
     * @param level
     * @param isOutline
     */
    public void send(IdentityHashMap<String, WebSocketServer> webSocketSet, String devcode,
                     String fileKey, String status,int position,String percent,boolean isInit, int level, boolean isOutline,int type) {
        for(String key : webSocketSet.keySet()){
            if(key.equals(devcode) && null != webSocketSet.get(key)){
                try {
                    Map<String, Object> map1 = new HashMap<>();
                    Map<String, Object> map2 = new HashMap<>();
                    map2.put("status", status);
                    map2.put("id", fileKey);
                    if(isInit){
                        map2.put("position",position);
                        map2.put("percent",percent);
                    }
                    if (isOutline) {
                        map2.put("level", level);
                    }
                    map1.put("data", map2);
                    map1.put("type", type);
                    ObjectMapper json = new ObjectMapper();
                    webSocketSet.get(key).sendMessage(json.writeValueAsString(map1));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Async
    public void copySameModel(String filekey, List<String> sameFileKeyList) {
        LOGGER.info(filekey + ":开始融合模型");
        final double startTime = System.nanoTime();
        List<String> allcomponentlist = new ArrayList<String>();
        for (int i = 0; i < sameFileKeyList.size(); i++) {

            String samefilekey = sameFileKeyList.get(i);

            List<String> componentlist = new ArrayList<String>();
            BosFile bosFile = fileService.getFile(filekey);
            try {
//    			componentlist = copyComponent(filekey,samefilekey);

                Optional<BosModelList> bosModelList = bosModelListRepo.findByKey(samefilekey);
                if (bosModelList.isPresent()) {
                    LOGGER.info("bosModelList存在");
                    List<String> modellist = bosModelList.get().getComponentlist();

                    for (String componentkey : modellist) {
                        Optional<BosComponent> bosComponent = comRepo.findByKey(componentkey);
                        if (bosComponent.isPresent()) {
                            BosComponent newComponent = new BosComponent();

                            BosComponent component = bosComponent.get(); // 解决guid相同时,component无法添加的问题
                            componentlist.add(filekey + "_" + component.getGuid() + "_" + i);
                            newComponent.setKey(filekey + "_" + component.getGuid() + "_" + i);
                            newComponent.setGuid(component.getGuid());
                            newComponent.setName(component.getName());
                            newComponent.setMatrix(component.getMatrix());
                            newComponent.setGeometry(component.getGeometry());
                            newComponent.setType(component.getType());
                            newComponent.setLineid(component.getLineid());
                            newComponent.setParent(this.getGuidByComponentWithIndex(filekey, component.getParent(), i));
                            newComponent.setChildren(this.getGuidByComponentListWithIndex(filekey, component.getChildren(), i));
                            newComponent.setSystemtype(component.getSystemtype());
                            newComponent.setModel("files/" + filekey);
                            comRepo.save(newComponent);

                            Optional<BosSystemGroup> system = bosSystemGroupRepo.findByKey(samefilekey + "_" + bosComponent.get().getGuid());
                            if (system.isPresent()) {
                                BosSystemGroup bosSystemGroup = new BosSystemGroup();
                                bosSystemGroup.setKey(filekey + "_" + component.getGuid() + "_" + i);
                                bosSystemGroup.setSystemgroup(this.getGuidByComponentList(filekey, system.get().getSystemgroup()));
                                bosSystemGroupRepo.save(bosSystemGroup);
                            }

                            Optional<BosHoseConnect> connect = bosHoseConnectRepo.findByKey(samefilekey + "_" + bosComponent.get().getGuid());
                            if (connect.isPresent()) {
                                BosHoseConnect bosHoseConnect = new BosHoseConnect();
                                bosHoseConnect.setKey(filekey + "_" + component.getGuid() + "_" + i);
                                bosHoseConnect.setHoseconnect(this.getGuidByComponentList(filekey, connect.get().getHoseconnect()));
                                bosHoseConnect.setSystempoint(this.getGuidByComponentWithIndex(filekey, connect.get().getSystempoint(), i));
                                bosHoseConnectRepo.save(bosHoseConnect);
                            }

                            Optional<BosAttribute> oAttr = attributeRepo.findByKey(samefilekey + "_" + bosComponent.get().getGuid());
                            if (oAttr.isPresent()) {
                                BosAttribute newAttribute = new BosAttribute();
                                BosAttribute attribute = oAttr.get();
                                newAttribute.setKey(filekey + "_" + bosComponent.get().getGuid() + "_" + i);
                                newAttribute.setAttributes(attribute.getAttributes());
                                attributeRepo.save(newAttribute);
                            }
                        }
                    }
                } else {
                    LOGGER.info("bosModelList不存在");
                    Iterable<BosComponent> iterCom = comRepo.findByModel("files/" + samefilekey);

                    for (BosComponent bosComponent : iterCom) {
                        BosComponent newComponent = new BosComponent();

                        BosComponent component = bosComponent;
                        componentlist.add(filekey + "_" + component.getGuid() + "_" + i);
                        newComponent.setKey(filekey + "_" + component.getGuid() + "_" + i);
                        newComponent.setGuid(component.getGuid());
                        newComponent.setName(component.getName());
                        newComponent.setMatrix(component.getMatrix());
                        newComponent.setGeometry(component.getGeometry());
                        newComponent.setType(component.getType());
                        newComponent.setLineid(component.getLineid());
                        newComponent.setParent(this.getGuidByComponentWithIndex(filekey, component.getParent(), i));
                        newComponent.setChildren(this.getGuidByComponentListWithIndex(filekey, component.getChildren(), i));
                        newComponent.setSystemtype(component.getSystemtype());
                        newComponent.setModel("files/" + filekey);
                        comRepo.save(newComponent);

                        Optional<BosSystemGroup> system = bosSystemGroupRepo.findByKey(samefilekey + "_" + bosComponent.getGuid());
                        if (system.isPresent()) {
                            BosSystemGroup bosSystemGroup = new BosSystemGroup();
                            bosSystemGroup.setKey(filekey + "_" + component.getGuid() + "_" + i);
                            bosSystemGroup.setSystemgroup(this.getGuidByComponentList(filekey, system.get().getSystemgroup()));
                            bosSystemGroupRepo.save(bosSystemGroup);
                        }

                        Optional<BosHoseConnect> connect = bosHoseConnectRepo.findByKey(samefilekey + "_" + bosComponent.getGuid());
                        if (connect.isPresent()) {
                            BosHoseConnect bosHoseConnect = new BosHoseConnect();
                            bosHoseConnect.setKey(filekey + "_" + component.getGuid() + "_" + i);
                            bosHoseConnect.setHoseconnect(this.getGuidByComponentList(filekey, connect.get().getHoseconnect()));
                            bosHoseConnect.setSystempoint(this.getGuidByComponentWithIndex(filekey, connect.get().getSystempoint(), i));
                            bosHoseConnectRepo.save(bosHoseConnect);
                        }

                        Optional<BosAttribute> oAttr = attributeRepo.findByKey(samefilekey + "_" + bosComponent.getGuid());
                        if (oAttr.isPresent()) {
                            BosAttribute newAttribute = new BosAttribute();
                            BosAttribute attribute = oAttr.get();
                            newAttribute.setKey(filekey + "_" + bosComponent.getGuid() + "_" + i);
                            newAttribute.setAttributes(attribute.getAttributes());
                            attributeRepo.save(newAttribute);
                        }
                    }
                }
                allcomponentlist.addAll(componentlist);
            } catch (Exception e) {
                bosFile.setStatus("2");
                fileRepo.save(bosFile);
                LOGGER.info(filekey + ":模型融合失败，" + e.getMessage());
                return;
            }

            if (i == sameFileKeyList.size() - 1) {

                if (allcomponentlist.size() != 0) {
                    BosModelList bosModelList2 = new BosModelList();
                    bosModelList2.setComponentlist(allcomponentlist);
                    bosModelList2.setKey(bosFile.getKey());
                    bosModelListRepo.save(bosModelList2);
                    bosFile.setStatus("1");
                    fileRepo.save(bosFile);
                    LOGGER.info(filekey + ":模型融合成功");
                    final double endTime = System.nanoTime();
                    double mergeTime = (endTime - startTime) / 1.E9;
                    LOGGER.info(filekey + "模型融合时间：" + mergeTime);
                } else {
                    bosFile.setStatus("2");
                    fileRepo.save(bosFile);
                    LOGGER.info(filekey + ":模型融合失败");
                }
            }
        }
    }

    public List<String> copyComponent(String filekey, String samefilekey,IdentityHashMap<String, WebSocketServer> webSocketSet,String devcode) {
        Optional<BosModelList> bosModelList = bosModelListRepo.findByKey(samefilekey);
        fileService.sendPercent(filekey,webSocketSet,devcode,samefilekey);

        List<String> componentlist = new ArrayList<>();
        if (bosModelList.isPresent()) {
            List<String> modellist = bosModelList.get().getComponentlist();
            for (String componentkey : modellist) {
                Optional<BosComponent> bosComponent = comRepo.findByKey(componentkey);
                if (bosComponent.isPresent()) {
                    BosComponent newComponent = new BosComponent();

                    BosComponent component = bosComponent.get();
                    componentlist.add(filekey + "_" + component.getGuid());
                    newComponent.setKey(filekey + "_" + component.getGuid());
                    newComponent.setGuid(component.getGuid());
                    newComponent.setName(component.getName());
                    newComponent.setMatrix(component.getMatrix());
                    newComponent.setGeometry(component.getGeometry());
                    newComponent.setType(component.getType());
                    newComponent.setLineid(component.getLineid());
                    newComponent.setParent(this.getGuidByComponent(filekey, component.getParent()));
                    newComponent.setChildren(this.getGuidByComponentList(filekey, component.getChildren()));
                    newComponent.setSystemtype(component.getSystemtype());
                    newComponent.setModel("files/" + filekey);
                    comRepo.save(newComponent);

                    Optional<BosSystemGroup> system = bosSystemGroupRepo.findByKey(samefilekey + "_" + bosComponent.get().getGuid());
                    if (system.isPresent()) {
                        BosSystemGroup bosSystemGroup = new BosSystemGroup();
                        bosSystemGroup.setKey(filekey + "_" + component.getGuid());
                        bosSystemGroup.setSystemgroup(this.getGuidByComponentList(filekey, system.get().getSystemgroup()));
                        bosSystemGroupRepo.save(bosSystemGroup);
                    }

                    Optional<BosHoseConnect> connect = bosHoseConnectRepo.findByKey(samefilekey + "_" + bosComponent.get().getGuid());
                    if (connect.isPresent()) {
                        BosHoseConnect bosHoseConnect = new BosHoseConnect();
                        bosHoseConnect.setKey(filekey + "_" + component.getGuid());
                        bosHoseConnect.setHoseconnect(this.getGuidByComponentList(filekey, connect.get().getHoseconnect()));
                        bosHoseConnect.setSystempoint(this.getGuidByComponent(filekey, connect.get().getSystempoint()));
                        bosHoseConnectRepo.save(bosHoseConnect);
                    }

                    Optional<BosAttribute> oAttr = attributeRepo.findByKey(samefilekey + "_" + bosComponent.get().getGuid());
                    if (oAttr.isPresent()) {
                        BosAttribute newAttribute = new BosAttribute();
                        BosAttribute attribute = oAttr.get();
                        newAttribute.setKey(filekey + "_" + bosComponent.get().getGuid());
                        newAttribute.setAttributes(attribute.getAttributes());
                        attributeRepo.save(newAttribute);
                    }

                }
            }
        } else {
            Iterable<BosComponent> iterCom = comRepo.findByModel("files/" + samefilekey);

            for (BosComponent bosComponent : iterCom) {
                BosComponent newComponent = new BosComponent();

                BosComponent component = bosComponent;
                componentlist.add(filekey + "_" + component.getGuid());
                newComponent.setKey(filekey + "_" + bosComponent.getGuid());
                newComponent.setGuid(component.getGuid());
                newComponent.setName(component.getName());
                newComponent.setMatrix(component.getMatrix());
                newComponent.setGeometry(component.getGeometry());
                newComponent.setType(component.getType());
                newComponent.setLineid(component.getLineid());
                newComponent.setParent(this.getGuidByComponent(filekey, component.getParent()));
                newComponent.setChildren(this.getGuidByComponentList(filekey, component.getChildren()));
                newComponent.setSystemtype(component.getSystemtype());

                Optional<BosSystemGroup> system = bosSystemGroupRepo.findByKey(samefilekey + "_" + bosComponent.getGuid());
                if (system.isPresent()) {
                    BosSystemGroup bosSystemGroup = new BosSystemGroup();
                    bosSystemGroup.setKey(filekey + "_" + component.getGuid());
                    bosSystemGroup.setSystemgroup(this.getGuidByComponentList(filekey, system.get().getSystemgroup()));
                    bosSystemGroupRepo.save(bosSystemGroup);
                }

                Optional<BosHoseConnect> connect = bosHoseConnectRepo.findByKey(samefilekey  + "_" + bosComponent.getGuid());
                if (connect.isPresent()) {
                    BosHoseConnect bosHoseConnect = new BosHoseConnect();
                    bosHoseConnect.setKey(filekey + "_" + component.getGuid());
                    bosHoseConnect.setHoseconnect(this.getGuidByComponentList(filekey, connect.get().getHoseconnect()));
                    bosHoseConnect.setSystempoint(this.getGuidByComponent(filekey, connect.get().getSystempoint()));
                    bosHoseConnectRepo.save(bosHoseConnect);
                }

                newComponent.setModel("files/" + filekey);
                comRepo.save(newComponent);
                Optional<BosAttribute> oAttr = attributeRepo.findByKey(samefilekey + "_" + bosComponent.getGuid());
                if (oAttr.isPresent()) {
                    BosAttribute newAttribute = new BosAttribute();
                    BosAttribute attribute = oAttr.get();
                    newAttribute.setKey(filekey + "_" + bosComponent.getGuid());
                    newAttribute.setAttributes(attribute.getAttributes());
                    attributeRepo.save(newAttribute);
                }
            }
        }
        fileService.saveSpaceTrees(filekey);
        return componentlist;
    }

    public String getGuidByComponentWithIndex(String filekey, String componentkey, Integer i) {
        if (componentkey != null) {
            String com[] = componentkey.split("_", 2);
            return filekey + "_" + com[1] + "_" + i;
        } else return null;
    }

    public List<String> getGuidByComponentListWithIndex(String filekey, List<String> componentlist, Integer i) {
        List<String> comList = new ArrayList<>();
        if (componentlist != null) {
            for (String component : componentlist) {
                String componentkey = this.getGuidByComponentWithIndex(filekey, component, i);
                comList.add(componentkey);
            }
            return comList;
        } else return null;
    }

    public String getGuidByComponent(String filekey, String componentkey) {
        if (componentkey != null) {
            String com[] = componentkey.split("_", 2);
            return filekey + "_" + com[1];
        } else return null;
    }

    public List<String> getGuidByComponentList(String filekey, List<String> componentlist) {
        List<String> comList = new ArrayList<>();
        if (componentlist != null) {
            for (String component : componentlist) {
                String componentkey = this.getGuidByComponent(filekey, component);
                comList.add(componentkey);
            }
            return comList;
        } else return null;
    }

    public void deleteComponentById(String id) {
        comRepo.deleteById(id);
    }

    @Async
    public void getRoadMap(String model, String vstxt, String cUnit, Double gridWidth, Double gridHeight, BosRoute bosRoute) throws IOException {

        final double startTime = System.nanoTime();
        LOGGER.info(model + ":开始提取路网");

        // 添加栅格提取信息
        BosGrids bosGrid = new BosGrids();
        bosGrid.setModel(model);
        bosGrid.setPath(null);
        GridSettings gridSetting = new GridSettings();
        gridSetting.setStatus("0.5");
        bosGrid.setGridSettings(gridSetting);
        bosGridsRepo.save(bosGrid);

        LOGGER.info(model + ":开始加载c++资源库");
        RoadMapJava roadMapJava = new RoadMapJava();
        roadMapJava.inits();
        LOGGER.info(model + ":调用c++初始化成功");
        roadMapJava.setUnits(cUnit);
        LOGGER.info(model + ":加载c++资源库，设置单位unit=" + cUnit);
        roadMapJava.setMainTxts(vstxt);
        //添加组件信息
        Gson gson = new Gson();
        List<BosComponent> list = gson.fromJson(vstxt, new TypeToken<List<BosComponent>>() {
        }.getType());
        LOGGER.info(model + ":开始添加组件信息，size=" + list.size());
        for (BosComponent bosComponent : list) {
            InputStream inputStream = FastDFS.downloadFileAsStream(bosComponent.getGeometry());
            String sLine = new BufferedReader(new InputStreamReader(inputStream))
                    .lines().collect(Collectors.joining(System.lineSeparator()));
            //添加组件信息
            roadMapJava.addComponentFromStrings(bosComponent.getKey(), sLine);
        }
        LOGGER.info(model + ":添加组件信息完毕");

        setWhsAndDoorDeltas(roadMapJava, cUnit, gridWidth, gridHeight);

        // 拓扑与栅格混合提取
        String computesGrid = "";
        try {
            computesGrid = roadMapJava.computePathTopologyGrids(model);
            LOGGER.info("computes=====" + computesGrid);
        } catch (Exception e) {
            saveGridsStatus(model);
            e.printStackTrace();
        }

        LOGGER.info("栅格地图文件：" + computesGrid);
        String stairGridName = model + "_stairGrid.csv";

        if (StringUtils.isEmpty(computesGrid) || stairGridName.equals(computesGrid)) {
            saveGridsStatus(model);
        } else {
            bosGridsRepo.deleteByModel(model);
            String[] fileNames = computesGrid.split(",");
            for (int i = 0; i < fileNames.length; i++) {
                if (!stairGridName.equals(fileNames[i])) {
                    saveGrid(model, fileNames[i]);
                } else {
                    saveStairGrid(model);
                }
            }
        }

        // 提取拓扑
        double[] xyz = roadMapJava.getXyzs();
        LOGGER.info(model + ":成功返回路网数据 xyz[] size=" + xyz.length);
        int[] index = roadMapJava.getIndexs();
        LOGGER.info(model + ":成功返回路网关联数据数据 index[] size=" + index.length);

        if (xyz.length == 0 || index.length == 0) {
            Map<String, Object> settings = bosRoute.getSettings();
            settings.put("status", BosConstants.ROUTE_STATUS_ERROR);
            bosRoute.setSettings(settings);
            routeRepo.save(bosRoute);
            LOGGER.info(model + ":拓扑路网提取失败");
        } else {
            savePoint(xyz, index, bosRoute, model);
        }

        //提取计时
        final double getOutlineEndTime = System.nanoTime();
        LOGGER.info(String.format(model + ":路网提取完成！共耗时 %.2f 秒。",
                (getOutlineEndTime - startTime) / 1.E9));
    }

    /**
     * 保存拓扑路网信息
     *
     * @param xyz
     * @param index
     * @param bosRoute1
     * @param model
     */
    public void savePoint(double[] xyz, int[] index, BosRoute bosRoute1, String model) {
        //根据fileKey查询参数信息
        Optional<BosRoute> byModel = routeRepo.findByModel(model);
        BosRoute bosRoute2 = byModel.get();

        Vector<vertexpoi> vecds = new Vector<vertexpoi>();
        //写入路网数据库
        //只有byRoute
        Optional<BosPoint> byRoute = pointRepo.findByRoute(bosRoute2.getId());
        if (!byRoute.isPresent()) {
            BosPoint bosPoint = null;
            vertexpoi vec = null;
            Map<Integer,String> map = new HashMap<>();
            for (int i = 0; i < xyz.length; i += 3) {
                vec = new vertexpoi();
                vec.setX((double) Math.round(xyz[i] * 1000) / 1000);
                vec.setY((double) Math.round(xyz[i + 1] * 1000) / 1000);
                vec.setZ((double) Math.round(xyz[i + 2] * 1000) / 1000);
                vecds.add(vec);
                //写入Point表
                bosPoint = new BosPoint();
                int i1 = i / 3;
                bosPoint.setX((double) Math.round(xyz[i] * 1000) / 1000);
                bosPoint.setY((double) Math.round(xyz[i + 1] * 1000) / 1000);
                bosPoint.setZ((double) Math.round(xyz[i + 2] * 1000) / 1000);
                bosPoint.setRoute(bosRoute1.getKey());
                BosPoint save = pointRepo.save(bosPoint);
                map.put(i1,save.getKey());
            }
            LOGGER.info(model + ":xyz路网数据保存完毕");
            BosEdge bosEdge = null;
            vertexpoi v1 = null;
            vertexpoi v2 = null;
            for (int i = 0; i < index.length; i += 2) {
                bosEdge = new BosEdge();
                //第一个点
                v1 = vecds.get(index[i]);
//                Optional<BosPoint> byXAndYAndXAndZ = pointRepo.findByXAndYAndZAndRoute(v1.getX(), v1.getY(), v1.getZ(), bosRoute1.getKey());
//                BosPoint bosPoint1 = byXAndYAndXAndZ.get();
                //第二个点
                v2 = vecds.get(index[i + 1]);
//                Optional<BosPoint> byXAndYAndXAndZ1 = pointRepo.findByXAndYAndZAndRoute(v2.getX(), v2.getY(), v2.getZ(), bosRoute1.getKey());
//                BosPoint bosPoint2 = byXAndYAndXAndZ1.get();
                //求出两个点之间的距离
                double distance = Math.sqrt((v1.getX() - v2.getX()) * (v1.getX() - v2.getX()) + ((v1.getY() - v2.getY()) * (v1.getY() - v2.getY())) + (v1.getZ() - v2.getZ()) * (v1.getZ() - v2.getZ()));
                //保留三位小数
                distance = (double) Math.round(distance * 1000) / 1000;
                //写入Edge表
                bosEdge.setFrom(map.get(index[i]));
                bosEdge.setTo(map.get(index[i+1]));
                bosEdge.setDistance(String.valueOf(distance));
                bosEdge.setRoute(bosRoute1.getKey());
                edgeRepo.save(bosEdge);
            }
            LOGGER.info(model + ":路网关联数据保存完毕");
            Optional<BosRoute> byModel2 = routeRepo.findByModel(model);
            BosRoute bosRoute = byModel2.get();
            Map<String, Object> settings = bosRoute.getSettings();
            settings.put("status", BosConstants.ROUTE_STATUS_SUCCESS);
            bosRoute.setSettings(settings);
            routeRepo.save(bosRoute);
            // 保存数据到fastdfs
            saveRouteAsZip(bosRoute);
        }
    }
    
    /**
     * 保存路网相关数据到fastdfs
     *
     * @param route
     * @param request
     * @param response
     */
    public void saveRouteAsZip(BosRoute bosRoute) {
        InputStream is = null;
        ServletOutputStream os = null;
        ZipOutputStream zout = null;

        File zipFile = new File("route_" + bosRoute.getKey() + ".zip");    // 定义压缩文件名称

        try {
            // 创建zip文件
            byte[] buf = new byte[1024];
            int len;
            zout = new ZipOutputStream(new FileOutputStream(zipFile));

            Iterable<BosPoint> bosPoints = routeService.getPointsByRouteId(bosRoute.getKey());
            List<Map<String, Object>> vertexpoiList = new ArrayList<Map<String, Object>>();
            Map<String, Object> map = null;
            for (BosPoint bosPoint : bosPoints) {
                map = new HashMap<String, Object>();
                map.put("x", bosPoint.getX());
                map.put("y", bosPoint.getY());
                map.put("z", bosPoint.getZ());
                map.put("key", bosPoint.getKey());
                vertexpoiList.add(map);
            }

            Iterable<BosEdge> bosEdges = routeService.getEdgesByRouteId(bosRoute.getKey());
            Map<String, Object> mapEdge = null;
            List<Map<String, Object>> mapList = new ArrayList<Map<String, Object>>();
            for (BosEdge bosEdge : bosEdges) {
                mapEdge = new HashMap<String, Object>();
                mapEdge.put("from", bosEdge.getFrom());
                mapEdge.put("to", bosEdge.getTo());
                mapEdge.put("distance", bosEdge.getDistance());
                mapList.add(mapEdge);
            }

            // 点
            is = new ByteArrayInputStream(com.alibaba.fastjson.JSONArray.parseArray(JSON.toJSONString(vertexpoiList)).toJSONString().getBytes("UTF-8"));
            zout.putNextEntry(new ZipEntry("points.json"));
            while ((len = is.read(buf)) > 0) {
                zout.write(buf, 0, len);
            }
            zout.closeEntry();

            // 边
            is = new ByteArrayInputStream(com.alibaba.fastjson.JSONArray.parseArray(JSON.toJSONString(mapList)).toJSONString().getBytes("UTF-8"));
            zout.putNextEntry(new ZipEntry("edges.json"));
            while ((len = is.read(buf)) > 0) {
                zout.write(buf, 0, len);
            }
            zout.closeEntry();

            String key = FastDFS.uploadFileByStream(new FileInputStream("route_" + bosRoute.getKey() + ".zip"), bosRoute.getKey() + ".zip", zipFile.length());
            Map<String, Object> settings = bosRoute.getSettings();
            settings.put("path", key);
            settings.put("size", zipFile.length());
            bosRoute.setSettings(settings);
            routeRepo.save(bosRoute);
            zipFile.delete();
            LOGGER.info(bosRoute.getModel() + ":拓扑路网数据保存fastdfs成功");

            zout.flush();
            zout.close();
            is.close();
        } catch (FileSystemException e) {
            throw e;
        } catch (IOException e) {
            throw new BosException(BosCodeEnum.INTERNAL_SERVER_ERROR, e);
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
        }
    }

    /**
     * 设置栅格宽高
     *
     * @param roadMapJava
     * @param cUnit
     * @param gridWidth:栅格宽度
     * @param gridHeight:栅格高度
     */
    public void setWhsAndDoorDeltas(RoadMapJava roadMapJava, String cUnit, Double gridWidth, Double gridHeight) {

        if (null != gridWidth && null != gridHeight) {
            roadMapJava.setPassableWhs(gridWidth, gridHeight);
            LOGGER.info("setPassableWhs设置成功 gridWidth=" + gridWidth + ",gridHeight=" + gridHeight);
        } else {
            switch (cUnit) { // setPassableWhs中gridWidth值设置为30cm、gridHeight值设置为1.5m doorDeltas设置为35cm,此处作一下转换
                case "0.1mm":
                    roadMapJava.setPassableWhs(3000, 15000);
                    roadMapJava.setDoorDeltas(3500);
                    LOGGER.info("setPassableWhs设置成功 gridWidth=3000,gridHeight=15000");
                    break;
                case "1mm":
                    roadMapJava.setPassableWhs(300, 1500);
                    roadMapJava.setDoorDeltas(350);
                    LOGGER.info("setPassableWhs设置成功 gridWidth=300,gridHeight=1500");
                    break;
                case "1cm":
                    roadMapJava.setPassableWhs(30, 150);
                    roadMapJava.setDoorDeltas(35);
                    LOGGER.info("setPassableWhs设置成功 gridWidth=30,gridHeight=150");
                    break;
                case "1dm":
                    roadMapJava.setPassableWhs(3, 15);
                    roadMapJava.setDoorDeltas(3.5);
                    LOGGER.info("setPassableWhs设置成功 gridWidth=3,gridHeight=15");
                    break;
                case "1m":
                    roadMapJava.setPassableWhs(0.3, 1.5);
                    roadMapJava.setDoorDeltas(0.35);
                    LOGGER.info("setPassableWhs设置成功 gridWidth=0.3,gridHeight=1.5");
                    break;
                case "1ft": // 1英尺(ft)=30.48厘米(cm)
                    roadMapJava.setPassableWhs(0.98425197, 150 / 30.48);
                    roadMapJava.setDoorDeltas(35 / 30.48);
                    LOGGER.info("setPassableWhs设置成功 gridWidth=0.98425197,gridHeight=" + 150 / 30.48);
                    break;
                case "1inch": // 1英寸(in)=2.54厘米(cm)
                    roadMapJava.setPassableWhs(11.8110236, 150 / 2.54);
                    roadMapJava.setDoorDeltas(35 / 2.54);
                    LOGGER.info("setPassableWhs设置成功 gridWidth=11.8110236,gridHeight=" + 150 / 2.54);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 通过给定的xyz值获取到最邻近的top个点
     *
     * @param route_id
     * @param x
     * @param y
     * @param z
     * @param top
     * @return
     */
    public List<String> getPointsNearByXyz(String point, String route_id, String x, String y, String z, Integer top) {
        //首先查出此模型所有的点
        Iterable<BosPoint> allByRoute = pointRepo.findAllByRoute(route_id);
        Map<Double, String> treeMap = new TreeMap<Double, String>();
        double x1 = new Double(0);
        double y1 = new Double(0);
        double z1 = new Double(0);
        if (StringUtils.isNotEmpty(point)) {
            Optional<BosPoint> byId = pointRepo.findByKey(point);
            if (byId.isPresent()) {
                BosPoint bosPoint = byId.get();
                x1 = bosPoint.getX();
                y1 = bosPoint.getY();
                z1 = bosPoint.getZ();
            } else
                return null;
        } else {
            x1 = Double.parseDouble(x);
            y1 = Double.parseDouble(y);
            z1 = Double.parseDouble(z);
        }
        for (BosPoint v2 : allByRoute) {
            //求出两个点之间的距离
            double distance = Math.sqrt((x1 - v2.getX()) * (x1 - v2.getX()) + ((y1 - v2.getY()) * (y1 - v2.getY())) + (z1 - v2.getZ()) * (z1 - v2.getZ()));
            //保留三位小数
            distance = (double) Math.round(distance * 1000) / 1000;
            treeMap.put(distance, v2.getKey());
        }
        Iterator<Double> iterator = treeMap.keySet().iterator();
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < treeMap.size(); i++) {
            if (i < top) {
                Double key = iterator.next();
                list.add(treeMap.get(key));
            } else
                break;
        }

        return list;
    }

    private int statu;

    /**
     * @Description: 根据用户的输入点返回查询到的合适栅格数据
     * @Param: [poi, unit]
     * @return: java.lang.Double
     * @Author: Wang
     * @Date: 2019/3/25
     */
    public Double getShortestFloor(double poi, String unit) {
        Double min = Double.POSITIVE_INFINITY;
        Double high = Double.POSITIVE_INFINITY;
        for (Double h : gridsMap.keySet()) {
            if (Math.abs((poi - h)) < min) {
                min = Math.abs((poi - h));
                high = h;
            }
        }
        this.setDistic(unit, min, 200.00, poi);
        return high;
    }

    /**
     * @Description: 根据用户的输入点返回查询到的第二临近的栅格数据
     * @Param: [poi, unit]
     * @return: java.lang.Double
     * @Author: Wang
     * @Date: 2019/3/25
     */
    public Double getSecondShortestFloor(double poi, String unit, Double pointH) {

        Map<Double, BosGrids> gridsTestMap = gridsMap;
        gridsTestMap.remove(pointH);

        Double min = Double.POSITIVE_INFINITY;
        Double high = Double.POSITIVE_INFINITY;
        for (Double h : gridsTestMap.keySet()) {
            if (Math.abs((poi - h)) < min) {
                min = Math.abs((poi - h));
                high = h;
            }
        }
        this.setDistic(unit, min, 200.00, poi);
        return high;
    }

    private void setDistic(String cUnit, Double distic, Double limitCM, Double pointHigh) {
        switch (cUnit) { // 设置离地距离限制
            case "0.1mm":
                if (null != distic && (distic < 0 || distic > limitCM * 100.0)) {
                    throw new BadRequestParameterException(BosCodeEnum.BAD_REQUEST.getCode(), "单位0.1mm时，gridWidth取值范围是 500-5000");
                }
                break;
            case "1mm":
                if (null != distic && (distic < 0 || distic > limitCM * 10)) {
                    throw new BadRequestParameterException(BosCodeEnum.BAD_REQUEST.getCode(), pointHigh + "高度处的点超出地面2m范围，请重新打点");
                }
                break;
            case "1cm":
                if (null != distic && (distic < 0 || distic > limitCM)) {
                    throw new BadRequestParameterException(BosCodeEnum.BAD_REQUEST.getCode(), pointHigh + "高度处的点超出地面2m范围，请重新打点");
                }
                break;
            case "1dm":
                if (null != distic && (distic < 0 || distic > limitCM / 10.0)) {
                    throw new BadRequestParameterException(BosCodeEnum.BAD_REQUEST.getCode(), pointHigh + "高度处的点超出地面2m范围，请重新打点");
                }
                break;
            case "1m":
                if (null != distic && (distic < 0 || distic > limitCM / 100.0)) {
                    throw new BadRequestParameterException(BosCodeEnum.BAD_REQUEST.getCode(), pointHigh + "高度处的点超出地面2m范围，请重新打点");
                }
                break;
            case "1ft": // 1英尺(ft)=30.48厘米(cm)
                if (null != distic && (distic < 0 || distic > limitCM / 30.48)) {
                    throw new BadRequestParameterException(BosCodeEnum.BAD_REQUEST.getCode(), pointHigh + "高度处的点超出地面2m范围，请重新打点");
                }
                break;
            case "1inch": // 1英寸(in)=2.54厘米(cm)
                if (null != distic && (distic < 0 || distic > limitCM / 2.54)) {
                    throw new BadRequestParameterException(BosCodeEnum.BAD_REQUEST.getCode(), pointHigh + "高度处的点超出地面2m范围，请重新打点");
                }
                break;
            default:
                throw new BadRequestParameterException(BosCodeEnum.BAD_REQUEST.getCode(), "单位输入有误");
        }
    }

    //混合路网获取最短路径
    public List<vertexpoi> getHybridShortest(String point1, String point2, String route, String filekey) throws IOException {
        String[] split1 = point1.split(",");
        String[] split2 = point2.split(",");
        if (split1.length != 3 || split2.length != 3) {
            throw new BadRequestParameterException("请输入规范的始终点坐标");
        }

        vertexpoi vertexpoi1 = new vertexpoi();
        vertexpoi vertexpoi2 = new vertexpoi();

        vertexpoi1.setX(Double.parseDouble(split1[0]));
        vertexpoi1.setY(Double.parseDouble(split1[1]));
        vertexpoi1.setZ(Double.parseDouble(split1[2]));

        vertexpoi2.setX(Double.parseDouble(split2[0]));
        vertexpoi2.setY(Double.parseDouble(split2[1]));
        vertexpoi2.setZ(Double.parseDouble(split2[2]));

        this.getGrids(filekey);                                             //存储不同高度的栅格
        Optional<BosUnit> bosUnit = bosUnitRepository.findByKey(filekey);
        if (!bosUnit.isPresent() || bosUnit.get().getUnit() == null) {
            throw new BadRequestParameterException("单位未提取，请提取单位");
        }
        String unit = bosUnit.get().getUnit();
        Double h1 = this.getShortestFloor(vertexpoi1.getZ(), unit);
        Double h2 = this.getShortestFloor(vertexpoi2.getZ(), unit);

        Map<Double, List<String>> pointsbyhigh = new HashMap<>();          //按高度存储拓扑点
        Map<String, vertexpoi> pointsbykey = new HashMap<>();

        Iterable<BosPoint> allByRoute = pointRepo.findAllByRoute(route);
        for (BosPoint bosPoint : allByRoute) {
            vertexpoi vertexpoi = new vertexpoi();
            vertexpoi.setX(bosPoint.getX());
            vertexpoi.setY(bosPoint.getY());
            vertexpoi.setZ(bosPoint.getZ());
            Double high = bosPoint.getZ();
            if (pointsbyhigh.containsKey(high)) {
                pointsbyhigh.get(high).add(bosPoint.getKey());
                pointsbykey.put(bosPoint.getKey(), vertexpoi);
            } else {
                List<String> vertexpoiList = new ArrayList<>();
                vertexpoiList.add(bosPoint.getKey());
                pointsbyhigh.put(high, vertexpoiList);
                pointsbykey.put(bosPoint.getKey(), vertexpoi);
            }
        }

        List<String> h1key = pointsbyhigh.get(h1);
        if (h1key == null) {
            h1 = this.getSecondShortestFloor(vertexpoi1.getZ(), unit, h1);
            h1key = pointsbyhigh.get(h1);
        }
        List<String> h2key = pointsbyhigh.get(h2);
        if (h2key == null) {
            h2 = this.getSecondShortestFloor(vertexpoi2.getZ(), unit, h2);
            h2key = pointsbyhigh.get(h2);
        }
        if (h1key != null && h2key != null) {
            Entrance e1star = this.getPosition(vertexpoi1, h1);
            Entrance e2star = this.getPosition(vertexpoi2, h2);
            if (e1star == null || e2star == null) {
                throw new BadRequestParameterException("超出打点范围");
            }
            if (h1 == h2) {
                if (e1star.equals(e2star)) {
                    throw new BadRequestParameterException("始终点距离过近，请重新打点");
                }

                if(gridsMap.get(h1).getGridSettings().getRow()*gridsMap.get(h1).getGridSettings().getCol()<=30000)
                {
                    List<vertexpoi> pathInOneFloor = this.getPathInFloor(e1star, e2star, h1);

                    pathInOneFloor.add(0,vertexpoi1);
                    pathInOneFloor.add(vertexpoi2);
                    gridsMap.clear();
                    return pathInOneFloor;
                }


            }


            final double startTime = System.nanoTime();

            /**
             * 通过栅格位置反向查询拓扑点的key值
             * **/
            Map<Entrance, String> keybyentranceh1 = this.saveEntrance(h1, h1key, pointsbykey);

            //TODO 计算h1key中的point到起点的最短距离的三个点

            List<Entrance> path1 = this.hybridpathlist(keybyentranceh1, h1, e1star);

            //TODO 判断这三个点是否存在总的路径当中，若存在，取最邻近的值，计算起点到该点的栅格路径并续接到之前的路径上

            String starkey = keybyentranceh1.get(path1.get(path1.size() - 1));

            path1.remove(path1.size() - 1);
            List<vertexpoi> uppath = this.toVertexpoi(h1, path1);                    //起点路径

            final double path1Time = System.nanoTime();
            double slicingTime = (path1Time - startTime) / 1.E9;

            Map<Entrance, String> keybyentranceh2 = this.saveEntrance(h2, h2key, pointsbykey);
            List<Entrance> path2 = this.hybridpathlist(keybyentranceh2, h2, e2star);
            Collections.reverse(path2);
            String endkey = keybyentranceh2.get(path2.get(0));

            path2.remove(0);
            List<vertexpoi> downpath = this.toVertexpoi(h2, path2);                 //终点路径


            final double path2Time = System.nanoTime();
            double slicingTime2 = (path2Time - path1Time) / 1.E9;

            Iterable<BosEdge> edgesByRouteId = edgeRepo.findAllByRoute(route);
            List<String> list = new ArrayList<String>();
            for (BosEdge bosEdge : edgesByRouteId) {
                list.add(bosEdge.getFrom());
                list.add(bosEdge.getTo());
                list.add(bosEdge.getDistance());
            }
            GraphService graphService = new GraphService(String.join(",", list));
            List<Integer> shortestPath = graphService.getShortestPath(Integer.parseInt(starkey), Integer.parseInt(endkey));
            if (shortestPath.size() == 0) {
                throw new BadRequestParameterException("未能查询到拓扑可通路径");
            }

            List<vertexpoi> medpath = new ArrayList<>();                              //中间路径
            for (Integer integer : shortestPath) {
                Optional<BosPoint> byId = pointRepo.findByKey(integer.toString());
                if (byId.isPresent()) {
                    vertexpoi vertexpoi = new vertexpoi();
                    vertexpoi.setX(byId.get().getX());
                    vertexpoi.setY(byId.get().getY());
                    vertexpoi.setZ(byId.get().getZ());
                    medpath.add(vertexpoi);
                }
            }


            if(medpath.size()>6)
            {
                Entrance e1end = new Entrance();
                Entrance e2end = new Entrance();
                for (int i =0 ;i<2 ;i++)
                {
                    medpath.remove(0);

                }

                if(medpath.get(0).getZ() == h1&&medpath.get(medpath.size()-1).getZ() == h2) {
                    e1end = this.getPosition(medpath.get(0), h1);

                    for (int i = 0; i < 2; i++) {
                        medpath.remove(medpath.size() - 1);
                    }
                    e2end = this.getPosition(medpath.get(medpath.size() - 1), h1);
                    List<vertexpoi> pathA1 = this.getPathInFloor(e1star, e1end, h1);
                    List<vertexpoi> pathA2 = this.getPathInFloor(e2end, e2star, h2);

                    final double midTime = System.nanoTime();
                    double slicingTime3 = (midTime - path2Time) / 1.E9;
                    LOGGER.info("混合路径搜寻总时间" + (slicingTime3 + slicingTime2 + slicingTime));

                    pathA1.addAll(medpath);
                    pathA1.addAll(pathA2);
                    pathA1.add(0, vertexpoi1);
                    pathA1.add(vertexpoi2);
                    gridsMap.clear();
                    return pathA1;
                }

            }

            final double midTime = System.nanoTime();
            double slicingTime3 = (midTime - path2Time) / 1.E9;
            LOGGER.info("拓扑路径搜寻时间"+slicingTime3);
            LOGGER.info("混合路径搜寻总时间"+(slicingTime3+slicingTime2+slicingTime));

            uppath.addAll(medpath);
            uppath.addAll(downpath);
            uppath.add(0, vertexpoi1);
            uppath.add(vertexpoi2);
            gridsMap.clear();
            return uppath;

        } else {
            gridsMap.clear();
            LOGGER.info(filekey+" : 栅格与拓扑路网高度不统一");
            throw new BadRequestParameterException("未能查询到拓扑可通路径");
        }
    }


    private List<vertexpoi> getPathInFloor (Entrance e1,Entrance e2,Double floorHigh) throws IOException {
        List<vertexpoi> path = new ArrayList<>();
        InputStream is =  FastDFS.downloadFileAsStream(gridsMap.get(floorHigh).getPath());
        int[][] gridmap =  this.getCsvDataNew(is);


        MapInfo info=new MapInfo(gridmap,gridmap[0].length, gridmap.length,new Node(e1.getX(), e1.getY()), new Node(e2.getX(), e2.getY()));

        List<Entrance> entrancepath =new ArrayList<>();
        entrancepath = new GridService().start(info);
        for (Entrance entrance:entrancepath)
        {
            vertexpoi poi = new vertexpoi();
            poi.setX(((entrance.getY()+0.5)*gridsMap.get(floorHigh).getGridSettings().getGridHeight())+gridsMap.get(floorHigh).getGridSettings().getX());
            poi.setY(((entrance.getX()+0.5)*gridsMap.get(floorHigh).getGridSettings().getGridWidth())+gridsMap.get(floorHigh).getGridSettings().getY());
            poi.setZ(floorHigh);
            path.add(poi);
        }
        if(path.isEmpty())
        {
            throw new BadRequestParameterException("无可达路径");
        }
        else
        {
            return path;
        }
    }

    public Optional<BosModelList> getModelList(String filekey) {
        Optional<BosModelList> modelList = modelListRepository.findByKey(filekey);
        return modelList;
    }

    /**
     * 通过filekey查询并将信息存储到gridmap中
     */
    private void getGrids(String filekey) {
        Iterable<BosGrids> grids = bosGridsRepository.findByModelAndPathNotNull(filekey);

        for (BosGrids bosGrid : grids) {
            gridsMap.put(Double.valueOf(bosGrid.getGridSettings().getHeight()), bosGrid);
        }
    }

    public int[][] getCsvDataNew(InputStream inputStream) throws IOException {

        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        String line = "";
        ArrayList<int[]> lineList = new ArrayList<int[]>();
        // Read a single line from the file until there are no more lines to read
        while ((line = br.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(line, ",");
            COLUMN_NUM = st.countTokens();
            int[] currCol = new int[COLUMN_NUM];
            for (int i = 0; i < COLUMN_NUM; i++) {
                if (st.hasMoreTokens()) {
                    currCol[i] = Integer.parseInt(st.nextToken());
                }
            }
            lineList.add(currCol);
        }

        int[][] str = new int[lineList.size()][COLUMN_NUM];
        for (int i = 0; i < lineList.size(); i++) {
            for (int j = 0; j < COLUMN_NUM; j++) {
                str[i][j] = lineList.get(i)[j];
                //System.out.println(str[i][x]);
            }
        }

        br.close();

        return str;
    }

    private Entrance getPosition(vertexpoi poi, Double high) {
        Entrance entrance = new Entrance();

        entrance.setX((int) Math.floor((poi.getY() - gridsMap.get(high).getGridSettings().getY()) / gridsMap.get(high).getGridSettings().getGridWidth()));
        entrance.setY((int) Math.floor((poi.getX() - gridsMap.get(high).getGridSettings().getX()) / gridsMap.get(high).getGridSettings().getGridHeight()));
        if (entrance.getX() >= 0 && entrance.getX() <= gridsMap.get(high).getGridSettings().getCol() && entrance.getY() >= 0 && entrance.getY() <= gridsMap.get(high).getGridSettings().getRow()) {
            return entrance;
        } else return null;

    }

    private Map<Entrance, String> saveEntrance(Double high, List<String> keys, Map<String, vertexpoi> pointsbykey) { //将entrance与key的关系相关联
        Map<Entrance, String> keybyentrance = new HashMap<>();
        for (String key : keys) {
            vertexpoi vertexpoi = pointsbykey.get(key);
            Entrance entrance = this.getPosition(vertexpoi, high);
            if (entrance != null) {
                keybyentrance.put(entrance, key);
            }
        }
        return keybyentrance;
    }

    private List<Entrance> hybridpathlist(Map<Entrance, String> keybyentranceh, Double high, Entrance star) throws IOException {
        List<Entrance> path = new ArrayList<>();
        InputStream is = FastDFS.downloadFileAsStream(gridsMap.get(high).getPath());
        int[][] gridmap = this.getCsvDataNew(is);
        int a = gridmap[star.getY()][star.getX()];
        if (a == 0) {
            throw new BadRequestParameterException("高度为：" + high + "处的打点为障碍物点");
        }
        List<Node> entrances = new ArrayList<>();
        for (Entrance entrance : keybyentranceh.keySet()) {
            entrances.add(new Node(entrance.getX(), entrance.getY()));
        }
        MapInformation info = new MapInformation(gridmap, gridmap[0].length, gridmap.length, new Node(star.getX(), star.getY()), entrances);
        path = new HybridRoteService().start(info);

        if (path.size() == 0) {
            LOGGER.info("高度为：" + high + "未能搜寻到拓扑叶子节点");
            throw new BadRequestParameterException("未能查询到拓扑可通路径");
        }

        return path;
    }

    private List<vertexpoi> toVertexpoi(Double floorHigh, List<Entrance> entrancepath) {
        List<vertexpoi> path = new ArrayList<>();
        for (Entrance entrance : entrancepath) {
            vertexpoi poi = new vertexpoi();
            poi.setX(((entrance.getY() + 0.5) * gridsMap.get(floorHigh).getGridSettings().getGridHeight()) + gridsMap.get(floorHigh).getGridSettings().getX());
            poi.setY(((entrance.getX() + 0.5) * gridsMap.get(floorHigh).getGridSettings().getGridWidth()) + gridsMap.get(floorHigh).getGridSettings().getY());
            poi.setZ(floorHigh);
            path.add(poi);
        }
        return path;
    }


    public List<List<Map<String, Object>>> getShortest(String point1, String point2, String route) {
        final double startTime = System.nanoTime();
        String proximalPoint = getProximalPoint(point1, point2, route);
        if ("".equals(proximalPoint)) {
            statu = 1;
            return null;
        }
        if ("1".equals(proximalPoint)) {
            statu = 2;
            return null;
        }
        String[] split = proximalPoint.split(",");

        Iterable<BosEdge> edgesByRouteId = edgeRepo.findAllByRoute(route);
        List<String> list = new ArrayList<String>();
        for (BosEdge bosEdge : edgesByRouteId) {
            list.add(bosEdge.getFrom());
            list.add(bosEdge.getTo());
            list.add(bosEdge.getDistance());
        }
        GraphService graphService = new GraphService(String.join(",", list));
        List<Integer> shortestPath = graphService.getShortestPath(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
        if (shortestPath.size() == 1) {
            statu = 3;
            return null;
        }
        List<List<Map<String, Object>>> lists = new ArrayList<List<Map<String, Object>>>();
        List<Map<String, Object>> list1 = null;
        Map<String, Object> map1 = null;
        Map<String, Object> map2 = null;
        for (int i = 0; i < shortestPath.size() - 1; i++) {
            list1 = new ArrayList<Map<String, Object>>();
            Optional<BosPoint> byId = pointRepo.findByKey(shortestPath.get(i).toString());
            if (byId.isPresent()) {
                map1 = new HashMap<String, Object>();
                BosPoint bosPoint = byId.get();
                map1.put("x", bosPoint.getX());
                map1.put("y", bosPoint.getY());
                map1.put("z", bosPoint.getZ());
                map1.put("key", bosPoint.getKey());
                list1.add(map1);
            }
            Optional<BosPoint> byId2 = pointRepo.findByKey(shortestPath.get(i + 1).toString());
            if (byId2.isPresent()) {
                map2 = new HashMap<String, Object>();
                BosPoint bosPoint = byId2.get();
                map2.put("x", bosPoint.getX());
                map2.put("y", bosPoint.getY());
                map2.put("z", bosPoint.getZ());
                map2.put("key", bosPoint.getKey());
                list1.add(map2);
            }
            lists.add(list1);
        }
        final double getOutlineEndTime = System.nanoTime();
        LOGGER.info(String.format("拓扑最短路径提取完成！共耗时 %.2f 秒。",
                (getOutlineEndTime - startTime) / 1.E9));
        return lists;
    }


    public String getProximalPoint(String point1, String point2, String route) {
        double x1 = new Double(0), x2 = new Double(0);
        double y1 = new Double(0), y2 = new Double(0);
        double z1 = new Double(0), z2 = new Double(0);
        if (point1.split(",").length == 1) {
            Optional<BosPoint> byId = pointRepo.findByKey(point1);
            if (byId.isPresent()) {
                BosPoint bosPoint = byId.get();
                x1 = bosPoint.getX();
                y1 = bosPoint.getY();
                z1 = bosPoint.getZ();
            } else
                return "";
        } else {
            x1 = Double.parseDouble(point1.split(",")[0]);
            y1 = Double.parseDouble(point1.split(",")[1]);
            z1 = Double.parseDouble(point1.split(",")[2]);
        }
        if (point2.split(",").length == 1) {
            Optional<BosPoint> byId = pointRepo.findByKey(point2);
            if (byId.isPresent()) {
                BosPoint bosPoint = byId.get();
                x2 = bosPoint.getX();
                y2 = bosPoint.getY();
                z2 = bosPoint.getZ();
            } else
                return "";
        } else {
            x2 = Double.parseDouble(point2.split(",")[0]);
            y2 = Double.parseDouble(point2.split(",")[1]);
            z2 = Double.parseDouble(point2.split(",")[2]);
        }

        Iterable<BosPoint> allByRoute = pointRepo.findAllByRoute(route);
        Map<Double, String> treeMap1 = new TreeMap<Double, String>();
        Map<Double, String> treeMap2 = new TreeMap<Double, String>();
        for (BosPoint v2 : allByRoute) {
            //求出两个点之间的距离
            double distance1 = Math.sqrt((x1 - v2.getX()) * (x1 - v2.getX()) + ((y1 - v2.getY()) * (y1 - v2.getY())) + (z1 - v2.getZ()) * (z1 - v2.getZ()));
            //保留三位小数
            distance1 = (double) Math.round(distance1 * 1000) / 1000;
            treeMap1.put(distance1, v2.getKey());

            //求出两个点之间的距离
            double distance2 = Math.sqrt((x2 - v2.getX()) * (x2 - v2.getX()) + ((y2 - v2.getY()) * (y2 - v2.getY())) + (z2 - v2.getZ()) * (z2 - v2.getZ()));
            //保留三位小数
            distance2 = (double) Math.round(distance2 * 1000) / 1000;
            treeMap2.put(distance2, v2.getKey());
        }
        if (treeMap1.size() <= 0) {
            return "1";
        }
        Iterator<Double> iterator = treeMap1.keySet().iterator();
        String str = "";
        for (int i = 0; i < treeMap1.size(); i++) {
            Double key = iterator.next();
            str += treeMap1.get(key);
            break;
        }

        Iterator<Double> iterator2 = treeMap2.keySet().iterator();
        for (int i = 0; i < treeMap2.size(); i++) {
            Double key = iterator2.next();
            str += "," + treeMap2.get(key);
            break;
        }
        return str;
    }

    public int getStatu() {
        return statu;
    }

    /**
     * 遍历查找所有children装到集合里面去
     *
     * @param childrenComList
     * @return
     */
    public List<BosComponentResult> getTreeList(List<String> childrenComList) {

        List<BosComponentResult> jsonClildrenlist = new ArrayList<BosComponentResult>();
        for (String comkey : childrenComList) {
            Optional<BosComponent> bosComponent = findBosComponentByKey(comkey);

            BosComponentResult componentResult = new BosComponentResult();
            try {
            	if (!bosComponent.isPresent()) { // 若bosComponent不存在，则跳出
					continue;
				}
                BeanUtils.copyProperties(componentResult, bosComponent.get());
                jsonClildrenlist.add(componentResult);
                List<String> childrenKeyList = componentResult.getChildren();
                if (null != childrenKeyList) {
                    componentResult.setChildrenResultList(getTreeList(childrenKeyList));
                    componentResult.setRoot(false);
                }
            } catch (Exception e) {
                e.printStackTrace();
                // TODO: handle exception
            }
        }
        return jsonClildrenlist;
    }

    /**
     * 根据指定componentKey列表，批量获取构件属性信息
     *
     * @param comList
     * @return
     */
    public BosResponseEntity findBosComponentByComList(List<String> comList) {

        if (comList.isEmpty()) {
            throw new BadRequestParameterException("构件列表为空");
        }

        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

        Iterable<BosComponent> oCom = comRepo.findByKeyIn(comList);
        Iterable<BosAttribute> oAttr = attributeRepo.findByKeyIn(comList);

        List<BosComponent> oComList = Lists.newArrayList(oCom);
        List<BosAttribute> oAttrList = Lists.newArrayList(oAttr);
        if (null == oComList || oComList.isEmpty()) {
            return new BosResponseEntity().setNotFound("找不到指定构件");
        }
        if (null == oAttrList || oAttrList.isEmpty()) {
            return new BosResponseEntity().setNotFound("构件属性信息为空");
        }

        for (BosAttribute bosAttribute : oAttr) {
            list.add(bosAttribute.getAttributes());
        }

        return new BosResponseEntity().setOk(list);
    }

    /**
     * 判断模型是否包含slab&wall 或者 plate&wall
     *
     * @param model
     * @return
     */
    public boolean judgeTypes(String model) {
        Iterable<String> iterCom = findModelTypes(model);
        ArrayList<String> list = Lists.newArrayList(iterCom);
        Pattern slab = Pattern.compile("slab", Pattern.CASE_INSENSITIVE);
        Pattern plate = Pattern.compile("plate", Pattern.CASE_INSENSITIVE);
        Pattern wall = Pattern.compile("wall", Pattern.CASE_INSENSITIVE);
        boolean flag1 = false;
        boolean flag2 = false;
        boolean flag3 = false;
        for (String type : list) {
            Matcher matSlab = slab.matcher(type);
            if (matSlab.find()) {
                flag1 = true;
            }
            Matcher matPlate = plate.matcher(type);
            if (matPlate.find()) {
                flag2 = true;
            }
            Matcher matWall = wall.matcher(type);
            if (matWall.find()) {
                flag3 = true;
            }
        }
        if (flag1 && flag3) {
            return true;
        }
        if (flag2 && flag3) {
            return true;
        }
        return false;
    }

    /**
     * 提取失败状态保存
     *
     * @param model
     */
    public void saveGridsStatus(String model) {
        //存入数据库
        Optional<BosGrids> bosGri = bosGridsRepo.findByModel(model);
        if (bosGri.isPresent()) {
            BosGrids bosGrids = bosGri.get();
            bosGrids.setModel(model);
            bosGrids.setPath(null);
            GridSettings gridSettings = new GridSettings();
            gridSettings.setStatus("2");
            bosGrids.setGridSettings(gridSettings);
            bosGridsRepo.save(bosGrids);
        }
        LOGGER.info("栅格地图提取失败");
    }

    /**
     * 保存栅格文件信息
     *
     * @param model
     * @param fileName
     */
    public void saveGrid(String model, String fileName) {
        File file = new File(gridFilePath + fileName);
        long size = file.length();
        if (size > 0) {
            String key = "";
            try {
                LOGGER.info("file.getSize()========" + file.length() + "filename========" + fileName + "size======" + size);
                key = FastDFS.uploadFileByStream(new FileInputStream(file), fileName, size);
                LOGGER.info("key==========" + key);
            } catch (Exception e) {
                LOGGER.info("提取失败" + e);
                saveGridStatus(model);
            }

            boolean flag = file.delete();
            LOGGER.info("栅格地图文件删除状态：" + flag);
            //存入数据库
            BosGrids bosGrids = new BosGrids();
            bosGrids.setModel(model);
            bosGrids.setPath(key);
            GridSettings gridSettings = new GridSettings();
            String[] split = fileName.split("_");
            gridSettings.setStatus("1");
            String[] split2 = model.split("_");
            if (split2.length == 2) {
                gridSettings.setUnit(split[2]);
                gridSettings.setX(Double.parseDouble(split[3]));
                gridSettings.setY(Double.parseDouble(split[4]));
                gridSettings.setHeight(Double.parseDouble(split[5]));
                gridSettings.setGridHeight(Double.parseDouble(split[6]));
                gridSettings.setGridWidth(Double.parseDouble(split[7]));
                gridSettings.setRow(Integer.parseInt(split[8]));
                String[] split1 = split[9].split("\\.");
                gridSettings.setCol(Integer.parseInt(split1[0]));
            } else {
                gridSettings.setUnit(split[1]);
                gridSettings.setX(Double.parseDouble(split[2]));
                gridSettings.setY(Double.parseDouble(split[3]));
                gridSettings.setHeight(Double.parseDouble(split[4]));
                gridSettings.setGridHeight(Double.parseDouble(split[5]));
                gridSettings.setGridWidth(Double.parseDouble(split[6]));
                gridSettings.setRow(Integer.parseInt(split[7]));
                String[] split1 = split[8].split("\\.");
                gridSettings.setCol(Integer.parseInt(split1[0]));
            }
            bosGrids.setGridSettings(gridSettings);
            bosGridsRepo.save(bosGrids);

            if (StringUtils.isEmpty(key)) {
                LOGGER.info("提取失败，key=" + key);
                saveGridStatus(model);
            }
        }
    }

    /**
     * 保存楼梯数据
     */
    public void saveStairGrid(String model) throws IOException {
        ArrayList<Double[]> csvDataNew = CsvDataUtil.getCsvDataNew(gridFilePath + model + "_stairGrid.csv");
        LOGGER.info("栅格地图楼梯数，size=" + csvDataNew.size());
        if (null != csvDataNew && !csvDataNew.isEmpty()) {
            for (int j = 0; j < csvDataNew.size(); j++) {
                BosStairGrid bosStairGrid = new BosStairGrid();
                bosStairGrid.setModel(model);
                int i1 = (int) Math.floor(csvDataNew.get(j)[0]);
                int i2 = (int) Math.floor(csvDataNew.get(j)[1]);
                List<vertexpoi> vertexpoiList = new ArrayList<vertexpoi>();
                for (int l = 6; l < csvDataNew.get(j).length; l += 3) {
                    vertexpoi vert = new vertexpoi();
                    vert.setX(csvDataNew.get(j)[l]);
                    vert.setY(csvDataNew.get(j)[l + 1]);
                    vert.setZ(csvDataNew.get(j)[l + 2]);
                    vertexpoiList.add(vert);
                }
                Entrance startEntrance = new Entrance();
                startEntrance.setX((int) Math.floor(csvDataNew.get(j)[3]));
                startEntrance.setY((int) Math.floor(csvDataNew.get(j)[2]));

                Entrance endEntrance = new Entrance();
                endEntrance.setX((int) Math.floor(csvDataNew.get(j)[5]));
                endEntrance.setY((int) Math.floor(csvDataNew.get(j)[4]));
                if (i1 > i2) {
                    bosStairGrid.setStartheight(i2);
                    bosStairGrid.setEndheight(i1);

                    bosStairGrid.setStartgrid(endEntrance);
                    bosStairGrid.setEndgrid(startEntrance);

                    Collections.reverse(vertexpoiList);
                } else {
                    bosStairGrid.setStartheight(i1);
                    bosStairGrid.setEndheight(i2);

                    bosStairGrid.setStartgrid(startEntrance);
                    bosStairGrid.setEndgrid(endEntrance);
                }
                bosStairGrid.setXyzlist(vertexpoiList);
                bosStairGridRepo.save(bosStairGrid);
            }
        }
        File files = new File(gridFilePath + model + "_stairGrid.csv");
        boolean flag = files.delete();
        LOGGER.info("栅格地图楼梯文件删除状态：" + flag);
    }

    public void saveGridStatus(String model) {
        //存入数据库
        Iterable<BosGrids> byModelAndPathNotNull = bosGridsRepo.findByModelAndPathNotNull(model);
        for (BosGrids bosGrids : byModelAndPathNotNull) {
            GridSettings gridSettings = new GridSettings();
            gridSettings.setStatus("2");
            bosGrids.setGridSettings(gridSettings);
            bosGridsRepo.save(bosGrids);
        }
    }

    /**
     * 获取栅格地图给定两个点之间的最短路径
     *
     * @param point1
     * @param point2
     * @return
     */
    public List<vertexpoi> gridShortest(String model, String point1, String point2) throws IOException {
        final double startTime = System.nanoTime();
        LOGGER.info(model + ":开始获取栅格地图最短路径");

        String[] split1 = point1.split(",");
        String[] split2 = point2.split(",");
        if (split1.length == 3 && split1.length == split2.length) {
            vertexpoi vertexpoi1 = new vertexpoi();
            vertexpoi vertexpoi2 = new vertexpoi();

            vertexpoi1.setX(Double.parseDouble(split1[0]));
            vertexpoi1.setY(Double.parseDouble(split1[1]));
            vertexpoi1.setZ(Double.parseDouble(split1[2]));

            vertexpoi2.setX(Double.parseDouble(split2[0]));
            vertexpoi2.setY(Double.parseDouble(split2[1]));
            vertexpoi2.setZ(Double.parseDouble(split2[2]));

            GridPathService gridPathService = new GridPathService(bosStairGridRepo, bosGridsRepo);

            Optional<BosUnit> bosUnit = bosUnitRepository.findByKey(model);
            if (!bosUnit.isPresent() || bosUnit.get().getUnit() == null) {
                throw new BadRequestParameterException("单位未提取，请提取单位");
            }
            String unit = bosUnit.get().getUnit();
            gridPathService.setGridPath(model, vertexpoi1, vertexpoi2, unit);
            List<vertexpoi> gridPathOverFloor = gridPathService.getGridPathOverFloor();

            LOGGER.info("最短路径点数=" + gridPathOverFloor.size());

            //提取计时
            final double getOutlineEndTime = System.nanoTime();
            LOGGER.info(String.format(model + ":获取栅格地图最短路径完成！共耗时 %.2f 秒。",
                    (getOutlineEndTime - startTime) / 1.E9));

            if (gridPathOverFloor.size() != 0)

            {
                gridPathOverFloor.add(0, vertexpoi1);
                gridPathOverFloor.add(vertexpoi2);
                return gridPathOverFloor;
            } else
                return new ArrayList<vertexpoi>();

        }
        return null;
    }

    public BosResponseEntity countSpot(String model) {
        Iterable<BosStairGrid> byModel = bosStairGridRepo.findByModel(model);
        Integer count = 0;
        for (BosStairGrid bosStairGrid : byModel) {
            List<vertexpoi> xyzlist = bosStairGrid.getXyzlist();
            count += xyzlist.size();
        }

        Iterable<BosGrids> byModelAndPathNotNull = bosGridsRepo.findByModelAndPathNotNull(model);
        for (BosGrids bosGrids : byModelAndPathNotNull) {
            GridSettings gridSettings = bosGrids.getGridSettings();
            count += gridSettings.getRow() * gridSettings.getCol();
        }

        JSONObject data = new JSONObject();
        data.put("count", count);
        return new BosResponseEntity().setOk(data);
    }

    public int countByModelAndGeometryIsNotNull(String id) {
        return comRepo.countByModelAndGeometryIsNotNull(id);
    }

    /**
     * 上传文件时异步添加lucene索引
     *
     * @param bosFile
     * @throws IOException
     */
    @Async
    public void saveIndexDir(BosFile bosFile) throws IOException {
        // 1. 采集数据
        if (bosFile.isCommunal()) {
            logger.info("上传文件同步Luence中，开始同步");
            // 2. 创建文档对象
            List<Document> documents = new ArrayList<Document>();
            Document document = new Document();
            // 给文档对象添加域
            // add方法: 把域添加到文档对象中, field参数: 要添加的域
            // TextField: 文本域, 属性name:域的名称, value:域的值, store:指定是否将域值保存到文档中
            document.add(new TextField("key", bosFile.getKey(), Field.Store.YES));
            document.add(new TextField("filename", bosFile.getName(), Field.Store.YES));
            if (null != bosFile.getTags() && !bosFile.getTags().isEmpty()) {
                document.add(new TextField("tags", bosFile.getTags().toString(), Field.Store.YES));
            }
            if (!org.springframework.util.StringUtils.isEmpty(bosFile.getDescription())) {
                document.add(new TextField("description", bosFile.getDescription(), Field.Store.YES));
            }
            //按照发布作者来筛选
            Optional<Account> accountById = accService.getAccountById(bosFile.getAccount());
            if (null != accountById && accountById.isPresent() && !org.springframework.util.StringUtils.isEmpty(accountById.get().getNickname())) {
                document.add(new TextField("nickname", accountById.get().getNickname(), Field.Store.YES));
            }

            // 将文档对象添加到文档对象集合中
            documents.add(document);
            logger.info("数据同步Luence中，共：" + documents.size() + " 条数据");
            // 3. 创建分析器对象(Analyzer), 用于分词
            Analyzer analyzer = new StandardAnalyzer();
            // 4. 创建索引配置对象(IndexWriterConfig), 用于配置Lucene
            // 参数一:当前使用的Lucene版本, 参数二:分析器
            IndexWriterConfig indexConfig = new IndexWriterConfig(Version.LUCENE_4_10_4, analyzer);
            // 5. 创建索引库目录位置对象(Directory), 指定索引库的存储位置
            File file = new File("indexDir");
            Directory directory = FSDirectory.open(file);

            // 6. 创建索引写入对象(IndexWriter), 将文档对象写入索引
            IndexWriter indexWriter = new IndexWriter(directory, indexConfig);
            // 7. 使用IndexWriter对象创建索引
            for (Document doc : documents) {
                // addDocement(doc): 将文档对象写入索引库
                indexWriter.addDocument(doc);
            }
            logger.info("数据同步Luence中，完成");
            // 8. 释放资源
            indexWriter.close();
        }
    }

    /**
     * 上传文件时异步添加索引
     *
     * @param bosFile
     * @throws IOException
     */
    @Async
    public void saveSpaceTrees(BosFile bosFile, boolean flag, List<BosComponentResult> jsonlist) throws IOException {
        List<BosComponentResult> bosComponentResults = flag == true ? jsonlist : getSpaceTree(bosFile);
        String tree = JSONArray.parseArray(JSON.toJSONString(bosComponentResults)).toJSONString();
        writeFile(tree);
        File file = new File("newfile.json");
        String key = FastDFS.uploadFileByStream(new FileInputStream("newfile.json"), bosFile.getKey() + ".json", file.length());
        BosSpaceTree bosSpaceTree = new BosSpaceTree();
        bosSpaceTree.setModel(bosFile.getId());
        bosSpaceTree.setTreepath(key);
        bosSpaceTree.setSize(file.length());
        bosSpaceTreeRepo.save(bosSpaceTree);
        file.delete();
    }

    public void writeFile(String content) {
        FileOutputStream fop = null;
        File file;
        try {
            file = new File("newfile.json");
            fop = new FileOutputStream(file);
            if (!file.exists()) {
                file.createNewFile();
            }
            byte[] contentInBytes = content.getBytes();
            fop.write(contentInBytes);
            fop.flush();
            fop.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fop != null) {
                    fop.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public String FormetFileSize(long file) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        if (file < 1024) {
            fileSizeString = df.format((double) file) + "B";
        } else if (file < 1048576) {
            fileSizeString = df.format((double) file / 1024) + "K";
        } else if (file < 1073741824) {
            fileSizeString = df.format((double) file / 1048576) + "M";
        } else {
            fileSizeString = df.format((double) file / 1073741824) + "G";
        }
        return fileSizeString;
    }


    /**
     * 删除文件时异步删除lucene索引
     *
     * @param key
     * @throws IOException
     */
    @Async
    public void deleteIndexDir(String key) throws IOException {
        // 创建目录对象
        Directory directory = FSDirectory.open(new File("indexDir"));
        // 创建配置对象
        IndexWriterConfig conf = new IndexWriterConfig(Version.LATEST, new StandardAnalyzer());
        // 创建索引写出工具
        IndexWriter writer = new IndexWriter(directory, conf);
        // 根据query对象删除
        //精确查询，根据名称来直接
        Query query = new TermQuery(new Term("key", key));
        writer.deleteDocuments(query);
        // 提交
        writer.commit();
        // 关闭
        writer.close();
    }

    //精确查找
    public boolean searchByTerm(String key, int num) {
        try {
            // 3. 创建分析器对象(Analyzer), 用于分词
            Analyzer analyzer = new StandardAnalyzer();
            // 索引目录对象
            Directory directory = FSDirectory.open(new File("indexDir"));
            // 索引读取工具
            IndexReader reader = DirectoryReader.open(directory);
            // 索引搜索工具
            IndexSearcher searcher = new IndexSearcher(reader);
            Query query = new TermQuery(new Term("key", key));
            TopDocs tds = searcher.search(query, num);
            if (tds.totalHits > 0) {
                return true;
            }
        } catch (CorruptIndexException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 模型对比
     *
     * @param model1
     * @param model2
     */
    private List<String> comList1;
    private List<String> comList2;

    @Async
    public void modelComparison(String model1, String model2, Boolean enforce, String accId, String name, BosFileComparison fileCom,IdentityHashMap<String, WebSocketServer> webSocketSet,String devcode) {
        final double startTime = System.nanoTime();
        LOGGER.info(model1 + "_" + model2 + ":开始进行模型对比");

        differenceCom(fileCom, model1, model2);

        Map<String, Object> attrmap = new HashMap<>();
        Map<String, Object> geomap = new HashMap<>();
        Map<String, Object> spacemap = new HashMap<>();
        Map<String, Object> connectmap = new HashMap<>();
        List<String> delList = new ArrayList<>();
        int count = 0;
        List<String> l = new ArrayList<>();
        l.addAll(comList2);
        l = getGuidByComponentLists(l);
        for (String comkey : comList1) {
            comkey = getGuidByComponents(comkey);
            if (l.contains(comkey)) {
                //对比属性
                Optional<BosAttribute> attrbyKey1 = attributeRepo.findByKey(model1 + "_" + comkey);
                Optional<BosAttribute> attrbyKey2 = attributeRepo.findByKey(model2 + "_" + comkey);
                if (attrbyKey1.isPresent()) {
                    if (attrbyKey2.isPresent()) {
                        Map<String, Object> attributes1 = attrbyKey1.get().getAttributes();
                        Map<String, Object> attributes2 = attrbyKey2.get().getAttributes();
                        Iterator<String> it = attributes1.keySet().iterator();
                        while (it.hasNext()) {
                            String key = it.next();
                            if (!attributes1.get(key).equals(attributes2.get(key))) {
                                attrmap.put(model1 + "_" + comkey, model2 + "_" + comkey);
                                continue;
                            }
                        }
                    } else {
                        attrmap.put(model1 + "_" + comkey, model2 + "_" + comkey);
                    }
                } else {
                    if (attrbyKey2.isPresent()) {
                        attrmap.put(model1 + "_" + comkey, model2 + "_" + comkey);
                    }
                }

                //对比几何
                Optional<BosComponent> byKey1 = comRepo.findByKey(model1 + "_" + comkey);
                BosComponent bosComponent1 = byKey1.get();

                Optional<BosComponent> byKey2 = comRepo.findByKey(model2 + "_" + comkey);
                BosComponent bosComponent2 = byKey2.get();
                InputStream inputStream1 = null;
                InputStream inputStream2 = null;
                try {
                    if (null != bosComponent1.getGeometry()) {
                        inputStream1 = FastDFS.downloadFileAsStream(bosComponent1.getGeometry());
                    }
                    if (null != bosComponent2.getGeometry()) {
                        inputStream2 = FastDFS.downloadFileAsStream(bosComponent2.getGeometry());
                    }
                } catch (Exception e) {
                    System.out.println("=========" + count);
                    e.printStackTrace();
                }
                if (null != inputStream1 && null != inputStream2 && !comGeometry(inputStream1, inputStream2)) {
                    geomap.put(model1 + "_" + comkey, model2 + "_" + comkey);
                }

                //对比空间关系
                if (!StringUtils.isEmpty(bosComponent1.getParent()) && !StringUtils.isEmpty(bosComponent2.getParent())) {
                    if (bosComponent1.getParent().split("_")[1].equals(bosComponent2.getParent().split("_")[1])) {
                        List<String> children1 = getGuidByComponentLists(bosComponent1.getChildren());
                        List<String> children2 = getGuidByComponentLists(bosComponent2.getChildren());
                        comList(children1, children2, spacemap, model1, model2, comkey);
                    } else {
                        spacemap.put(model1 + "_" + comkey, model2 + "_" + comkey);
                    }
                } else if (StringUtils.isEmpty(bosComponent1.getParent()) && StringUtils.isEmpty(bosComponent2.getParent())) {
                } else {
                    spacemap.put(model1 + "_" + comkey, model2 + "_" + comkey);
                }

                //对比连接关系
                List<String> systemgroup1 = new ArrayList<>();
                Optional<BosSystemGroup> system1 = bosSystemGroupRepo.findByKey(bosComponent1.getKey());
                if (system1.isPresent()) {
                    systemgroup1 = getGuidByComponentLists(system1.get().getSystemgroup());
                }
                List<String> hoseconnect1 = new ArrayList<>();
                String systempoint1 = null;
                Optional<BosHoseConnect> connect1 = bosHoseConnectRepo.findByKey(bosComponent1.getKey());
                if (connect1.isPresent()) {
                    hoseconnect1 = getGuidByComponentLists(connect1.get().getHoseconnect());
                    systempoint1 = connect1.get().getSystempoint();
                }

                List<String> systemgroup2 = new ArrayList<>();
                Optional<BosSystemGroup> system2 = bosSystemGroupRepo.findByKey(bosComponent2.getKey());
                if (system2.isPresent()) {
                    systemgroup2 = getGuidByComponentLists(system2.get().getSystemgroup());
                }
                List<String> hoseconnect2 = new ArrayList<>();
                String systempoint2 = null;
                Optional<BosHoseConnect> connect2 = bosHoseConnectRepo.findByKey(bosComponent2.getKey());
                if (connect2.isPresent()) {
                    hoseconnect2 = getGuidByComponentLists(connect2.get().getHoseconnect());
                    systempoint2 = connect2.get().getSystempoint();
                }
                comList(systemgroup1, systemgroup2, connectmap, model1, model2, comkey);
                comList(hoseconnect1, hoseconnect2, connectmap, model1, model2, comkey);

                if (!StringUtils.isEmpty(systempoint1) && !StringUtils.isEmpty(systempoint2)) {
                    if (!systempoint1.equals(systempoint2)) {
                        connectmap.put(model1 + "_" + comkey, model2 + "_" + comkey);
                    }
                } else if (StringUtils.isEmpty(systempoint1) && StringUtils.isEmpty(systempoint2)) {
                } else {
                    connectmap.put(model1 + "_" + comkey, model2 + "_" + comkey);
                }
                comList2.remove(model2 + "_" + comkey);
            } else {
                delList.add(model1 + "_" + comkey);
            }
        }

        fileCom.setAddlist(comList2); //增加的构件
        fileCom.setDellist(delList); //删除的构件
        fileCom.setAttrmap(attrmap); //不同属性
        fileCom.setGeometrymap(geomap); //不同几何
        fileCom.setSpacemap(spacemap); //不同空间关系
        fileCom.setConnectmap(connectmap); //不同连接关系
        fileCom.setComendtime(DateTime.now().getMillis());
        fileCom.setStatus("1");
        fileComparisonRepo.save(fileCom);
        send(webSocketSet,devcode,fileCom.getKey(),"1",0,"",false,0,false,2);
        //提取计时
        final double getOutlineEndTime = System.nanoTime();
        LOGGER.info(String.format(model1 + "_" + model2 + ":模型对比完成！共耗时 %.2f 秒。",
                (getOutlineEndTime - startTime) / 1.E9));
    }

    /**
     * 更改文件名
     *
     * @param fileId:文件key
     * @param fileName:新的文件名
     * @return
     */
    public String reviseFileName(String accId, String fileName) {
        String name = "";
        for (int i = 0; i < 9999; i++) { // 修改重名的文件
            name = fileName + (i == 0 ? "" : "(" + i + ")");
            Iterable<BosFileComparison> it = fileComparisonRepo.findByAccountAndName(accId, name);
            if (!it.iterator().hasNext()) {
                break;
            }
        }
        return name;
    }

    /**
     * list集合对比
     *
     * @param children1
     * @param children2
     * @param spacemap
     * @param model1
     * @param model2
     * @param comkey
     */
    public void comList
    (List<String> children1, List<String> children2, Map<String, Object> spacemap, String model1, String
            model2, String comkey) {
        if (null != children1 && null != children2) {
            List<String> removeList = new ArrayList<>();
            for (String children : children1) {
                if (!children2.isEmpty() && children2.contains(children)) {
                    removeList.add(children);
                } else {
                    spacemap.put(model1 + "_" + comkey, model2 + "_" + comkey);
//                    children2.clear();
                    continue;
                }
            }
            children2.removeAll(removeList);
        }
        if (null != children2 && !children2.isEmpty()) {
            spacemap.put(model1 + "_" + comkey, model2 + "_" + comkey);
        }
    }

    /**
     * 对比文件流
     *
     * @param inputStream1
     * @param inputStream2
     * @return
     */
    public boolean comGeometry(InputStream inputStream1, InputStream inputStream2) {
        try {
            byte[] b1 = new byte[inputStream1.available()];
            inputStream1.read(b1);
            byte[] b2 = new byte[inputStream2.available()];
            inputStream2.read(b2);
            String a1 = new String(b1);
            String a2 = new String(b2);
            if (a1.equals(a2)) {
                return true;
            }
            inputStream1.close();
            inputStream2.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 获取不同模型构件
     *
     * @param fileCom
     * @param model1
     * @param model2
     */
    public void differenceCom(BosFileComparison fileCom, String model1, String model2) {
        comList1 = new ArrayList<>();
        comList2 = new ArrayList<>();
        Optional<BosModelList> byKey1 = bosModelListRepo.findByKey(model1);
        if (byKey1.isPresent()) {
            comList1.addAll(byKey1.get().getComponentlist());
        }

        Optional<BosModelList> byKey2 = bosModelListRepo.findByKey(model2);
        if (byKey2.isPresent()) {
            comList2.addAll(byKey2.get().getComponentlist());
        }
    }

    /**
     * 去除文件key
     *
     * @param componentkey
     * @return
     */
    public String getGuidByComponents(String componentkey) {
        if (componentkey != null) {
            String com[] = componentkey.split("_", 2);
            return com[1];
        } else return null;
    }

    public List<String> getGuidByComponentLists(List<String> componentlist) {
        List<String> comList = new ArrayList<>();
        if (componentlist != null) {
            for (String component : componentlist) {
                String componentkey = this.getGuidByComponents(component);
                comList.add(componentkey);
            }
            return comList;
        } else return null;
    }

    public BosResponseEntity getCount(String model) {
        Map<String, Object> map = getCountNumberMap(model);
        return new BosResponseEntity().setOk(map);
    }

    public Map<String, Object> getCountNumberMap(String model) {
        Map<String, Object> map = new HashMap<>();
        //统计构件数量
        int count1 = comRepo.countByModel("files/" + model);
        map.put("comCount", String.valueOf(count1));
        //统计属性数量
        int count2 = 0;
        Iterable<BosComponent> byModel = comRepo.findByModel("files/" + model);
        for (BosComponent bosComponent : byModel) {
            Optional<BosAttribute> byKey = attributeRepo.findByKey(bosComponent.getKey());
            if (byKey.isPresent()) {
                count2++;
            }
        }
        map.put("attrCount", String.valueOf(count2));
        //统计几何数量
        int count3 = comRepo.countByModelAndGeometryIsNotNull("files/" + model);
        map.put("geoCount", String.valueOf(count3));
        //统计gltf数量
        int count4 = 0;
        Optional<BosGltf> bosGltf = gltfRepo.findByModel("file/" + model);
        if (bosGltf.isPresent()) {
            BosGltf bosGltf1 = bosGltf.get();
            if ("1".equals(bosGltf1.getStatus())) {
                map.put("gltfCount", "1");
            } else {
                map.put("gltfCount", "0.5");
            }
        } else {
            map.put("gltfCount", "0.5");
        }

        // 统计外轮廓数量
        for (int level = 1; level <= 3; level++) {
            if ("1".equals(getOutlineStatus(model, level))) {

                getOutlinesCount(map, model, level);
            } else {
                map.put("outlineCount" + level, "0.5");
            }
        }

        return map;
    }

    public void getOutlinesCount(Map<String, Object> map, String model, int type) {
        Optional<BosOutline> byKey = outlineRepo.findByKey(model + "_" + type);
        if (byKey.isPresent()) {
            Vector<String> outlines = byKey.get().getOutlines();
            if (null != outlines && !outlines.isEmpty()) {
                map.put("outlineCount" + type, String.valueOf(outlines.size()));
            } else {
                map.put("outlineCount" + type, "0.5");
            }
        }
    }

    /**
     * 获取outline
     *
     * @param model
     * @param level
     * @return
     */
    public Optional<BosOutline> getOutlineByKey(String model, Integer level) {
        return outlineRepo.findByKey(model + "_" + level);
    }

    /**
     * 获取外轮廓提取状态
     *
     * @param model
     * @param level
     * @return
     */
    public String getOutlineStatus(String model, Integer level) {
        Optional<BosOutline> outline = outlineRepo.findByKey(model + "_" + level);
        if (outline.isPresent()) {
            return outline.get().getStatus();
        }
        return "0";
    }
    
    public Vector<String> getOutlines(String model, Integer level) {
        Optional<BosOutline> outline = outlineRepo.findByKey(model + "_" + level);
        if (outline.isPresent()) {
            return outline.get().getOutlines();
        }
        return null;
    }


    /**
     * 删除外轮廓
     *
     * @param model
     * @param level
     */
    public void deleteOutlineById(String model, Integer level) {
        outlineRepo.deleteById(model + "_" + level);
    }

    /**
     * 判断模型是否正在提取外轮廓
     *
     * @param model
     * @return
     */
    public boolean isParsingOutline(String model) {

        boolean flag = false;

        Iterable<BosOutline> outlineIter = outlineRepo.findByModel(model);
        for (BosOutline bosOutline : outlineIter) {
            if ("0.5".equals(bosOutline.getStatus())) {
                flag = true;
            }
        }
        return flag;
    }

    /**
     * 处理老的外轮廓
     *
     * @param model
     */
    public void changeOldOutline(String model) {
        Optional<BosOutline> bosOutline_Old = outlineRepo.findByKey(model); // 老的外轮廓
        if (bosOutline_Old.isPresent()) {
            BosOutline _b = bosOutline_Old.get();
            BosOutline _tmp = new BosOutline();
            _tmp.setKey(model + "_1");
            _tmp.setLevel(1);
            _tmp.setModel(model);
            _tmp.setOutlines(_b.getOutlines());
            outlineRepo.delete(_b);
            outlineRepo.save(_tmp);
        }
    }

    /**
     * 检测模型列表中单位是否相同
     *
     * @param modelList
     * @return
     */
    public boolean checkModelUnitIsSame(List<String> modelList) {

        boolean boo = true;

        Optional<BosUnit> bosUnit = bosUnitRepository.findByKey(modelList.get(0));
        String unit = "";
        if (bosUnit.isPresent() && bosUnit.get().getUnit() != null) {
            unit = bosUnit.get().getUnit();
        }

        for (String model : modelList) {
            if (!unit.equals(bosUnitRepository.findByKey(model).get().getUnit())) {
                boo = false;
            }
        }

        return boo;
    }

    /**
     * 检测模型单位是否已提取
     *
     * @param model
     * @return
     */
    public boolean checkModelUnitIsExist(String model) {

        boolean boo = false;

        Optional<BosUnit> bosUnit = bosUnitRepository.findByKey(model);
        if (bosUnit.isPresent() && bosUnit.get().getUnit() != null) {
            boo = true;
        }
        return boo;
    }

    /**
     * 获取空间树
     *
     * @param model 文件key
     * @return
     */
//    @Async
    public List<BosComponentResult> getSpaceTree(BosFile file) {
        List<BosComponentResult> jsonlist = new ArrayList<BosComponentResult>();
        // 此处要确认是否都是IFCPROJECT
        Iterable<BosComponent> comIte = comRepo.findByModelAndType(file.getId(), "IFCPROJECT");
        for (BosComponent bosComponent : comIte) {
            try {
                BosComponentResult componentResult = new BosComponentResult();
                BeanUtils.copyProperties(componentResult, bosComponent);
                jsonlist.add(componentResult);
                List<String> childrenKeyList = componentResult.getChildren();
                componentResult.setChildrenResultList(getTreeLists(childrenKeyList));
                componentResult.setRoot(true);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
//        BosSpaceTree bosSpaceTree = new BosSpaceTree();
//        bosSpaceTree.setModel(file.getId());
//        bosSpaceTree.setJsonlist(jsonlist);
//        bosSpaceTreeRepo.save(bosSpaceTree);
        //file.getId()
        //jsonlist
        return jsonlist;
    }

    /**
     * 遍历查找所有children装到集合里面去
     *
     * @param childrenComList
     * @return
     */
    public List<BosComponentResult> getTreeLists(List<String> childrenComList) {

        List<BosComponentResult> jsonClildrenlist = new ArrayList<BosComponentResult>();
        for (String comkey : childrenComList) {
            Optional<BosComponent> bosComponent = comRepo.findByKey(comkey);
            BosComponentResult componentResult = new BosComponentResult();
            try {
                if(bosComponent.isPresent()) {
                    BeanUtils.copyProperties(componentResult, bosComponent.get());
                    jsonClildrenlist.add(componentResult);
                }
                List<String> childrenKeyList = componentResult.getChildren();
                if (null != childrenKeyList) {
                    componentResult.setChildrenResultList(getTreeList(childrenKeyList));
                    componentResult.setRoot(false);
                }
            } catch (Exception e) {
                e.printStackTrace();
                // TODO: handle exception
            }
        }
        return jsonClildrenlist;
    }

    /**
     * 根据条件查询构件信息
     *
     * @param model
     * @param body
     * @return
     */
    public Iterable<BosComponent> searchComponents(String model, String key, String type, String value, int pageIndex, int pageSize) {
        switch (key) {
            case "id":
                return getBosCom(model, type, value, "_id", pageIndex, pageSize);
            case "key":
                return getBosCom(model, type, value, "_key", pageIndex, pageSize);
            case "guid":
                return getBosCom(model, type, value, "guid", pageIndex, pageSize);
            case "name":
                return getBosCom(model, type, value, "name", pageIndex, pageSize);
            case "geometry":
                return getBosCom(model, type, value, "geometry", pageIndex, pageSize);
            case "type":
                return getBosCom(model, type, value, "type", pageIndex, pageSize);
            case "lineid":
                return getBosCom(model, type, value, "lineid", pageIndex, pageSize);
            case "model":
                return getBosCom(model, type, value, "model", pageIndex, pageSize);
            case "parent":
                return getBosCom(model, type, value, "parent", pageIndex, pageSize);
            case "systemtype":
                return getBosCom(model, type, value, "systemtype", pageIndex, pageSize);
            case "matrix":
                if("8".equals(type)){
                    return comRepo.findByModelAndMatrixIsInAndLimit(model, getValue(value), pageIndex, pageSize);
                }else if("9".equals(type)){
                    return comRepo.findByModelAndMatrixNotInAndLimit(model, getValue(value), pageIndex, pageSize);
                }else{
                    return getBosCom(model, type, value, "matrix", pageIndex, pageSize);
                }
            case "children":
                return getBosCom(model, type, value, "children", pageIndex, pageSize);
        }
        return null;
    }

    public Iterable<BosComponent> getBosCom(String model, String type, String value, String flag, int pageIndex, int pageSize) {
        switch (Integer.parseInt(type)) {
            case 1:
                return comRepo.findByModelAndValueAndLimit(model, value, flag, pageIndex, pageSize);
            case 2:
                return comRepo.findByModelAndValueIsNotAndLimit(model, value, flag, pageIndex, pageSize);
            case 3:
                return comRepo.findByModelAndValueIsLessAndLimit(model, value, flag, pageIndex, pageSize);
            case 4:
                return comRepo.findByModelAndValueIsLessOrIsAndLimit(model, value, flag, pageIndex, pageSize);
            case 5:
                return comRepo.findByModelAndValueIsGreaterAndLimit(model, value, flag, pageIndex, pageSize);
            case 6:
                return comRepo.findByModelAndValueIsGreaterIsAndLimit(model, value, flag, pageIndex, pageSize);
            case 7:
                return comRepo.findByModelAndValueIsLikeAndLimit(model, "%"+value+"%", flag, pageIndex, pageSize);
            case 8:
                return comRepo.findByModelAndValueIsInAndLimit(model, Arrays.asList(value.split(",")), flag, pageIndex, pageSize);
            case 9:
                return comRepo.findByModelAndValueIsNotInAndLimit(model, Arrays.asList(value.split(",")), flag, pageIndex, pageSize);
        }
        return null;
    }

    public double[] getValue(String value){
        String[] split = value.split(",");
        double[] matrix = new double[split.length];
        for(int i = 0; i< split.length;i++){
            matrix[i] = Double.parseDouble(split[i]);
        }
        return matrix;
    }

    @Async
    public void sendPercent(String key,IdentityHashMap<String, WebSocketServer> webSocketSet,String devcode) {
       fileService.initSendPercent(key,webSocketSet,devcode);
    }

    public int countByModel(String model) {
        return comRepo.countByModel(model);
    }
}