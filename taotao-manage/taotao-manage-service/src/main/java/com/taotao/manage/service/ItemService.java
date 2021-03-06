package com.taotao.manage.service;

import com.github.abel533.entity.Example;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.taotao.common.service.ApiService;
import com.taotao.manage.mapper.ItemMapper;
import com.taotao.pojo.Item;
import com.taotao.pojo.ItemDesc;
import com.taotao.pojo.ItemParamItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class ItemService extends BaseService<Item> {

    @Autowired
    private ItemDescService itemDescService;

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private ItemParamItemService itemParamItemService;

    @Autowired
    private ApiService apiService;

    @Value("${TAOTAO_WEB_URL}")
    private String TAOTAO_WEB_URL;

    // 同一个方法中存在两个事务，根据事务的传播特性，如果当前的事务存在，则另一个事务嵌套当前事务执行
    public void saveItem(Item item,String desc,String itemParams){
        // 设置初始数据
        item.setStatus(1);
        // 强制设置id为null，数据库主键自动增长
        item.setId(null);
        super.save(item);

        // 商品描述表（tb_item_desc）
        ItemDesc itemDesc = new ItemDesc();
        // 商品表（item）主键自增后返回增长后的id值
        itemDesc.setItemId(item.getId());
        itemDesc.setItemDesc(desc);
        // 保存描述数据
        this.itemDescService.save(itemDesc);

        // 保存商品规格
        ItemParamItem itemParamItem = new ItemParamItem();
        itemParamItem.setItemId(item.getId());
        itemParamItem.setParamData(itemParams);
        this.itemParamItemService.save(itemParamItem);
    }

    public PageInfo<Item> queryPageList(Integer page,Integer rows){
        Example example = new Example(Item.class);
        example.setOrderByClause("updated DESC");

        // 设置分页参数
        PageHelper.startPage(page,rows);

        List<Item> list = this.itemMapper.selectByExample(example);

        return new PageInfo<>(list);
    }

    public  void updateItem(Item item, String desc,String itemParams){
        // 强制设置不能修改的字段为null
        item.setStatus(null);
        item.setCreated(null);
        super.updateSelective(item);

        // 修改商品描述数据
        ItemDesc itemDesc = new ItemDesc();
        itemDesc.setItemId(item.getId());
        itemDesc.setItemDesc(desc);
        this.itemDescService.updateSelective(itemDesc);

        // 修改商品规格参数数据(根据传入的商品id进行选择性更新)
        this.itemParamItemService.updateItemParamItem(item.getId(),itemParams);
        try {
            // 通知其它系统，该商品已经更新
            String url = TAOTAO_WEB_URL + "/item/cache/" + item.getId() + ".html";
            this.apiService.doPost(url,null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
