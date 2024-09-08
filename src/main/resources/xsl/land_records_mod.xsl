<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<!-- 
		ЕСЛИ ВЫ РЕДАКТИРУЕТЕ ЭТОТ ДОКУМЕНТ В ECLIPSE, ОТКЛЮЧИТЕ ФОРМАТИРОВАНИЕ 
		КОММЕНТАРИЕВ Window / Preferences / XML / XML Files / Editor 
		
		Снимите флажок Format comments 
	-->

	<!-- 
		Хотя файл XSL допускает передачу значений в элемент/атрибут/параметр в формате 
		
		<xsl:attribute>value</xsl:attribute> 
		
		но при форматировании этого  документа будут добавлены отступы и переносы, которые попадут в результат 
		
		<xsl:attribute> value </xsl:attribute> 
		
		Рекомендуется использовать внутри контейнера инструкцию 
		
		<xsl:value-of select="'value'" /> 
		
		Тогда при форматировании  исходного документа отступы и переносы будут проигнорированы 
		
		<xsl:attribute> 
			<xsl:value-of select="'value'" /> 
		</xsl:attribute> 
		
	-->

	<!-- 
		Расскомментируйте следующую строку, если проверяете шаблон
		во внешних трансформаторах XSLT 
	-->
	
	
	<!-- Имя исходного файла -->
	<xsl:param name="source_file">
		<xsl:value-of select="'path'" />
	</xsl:param>
	
	<!-- Менять ли местами X и Y у координат (0 - false, 1 - true) -->
	<xsl:param name="swap_xy">
		<xsl:value-of select="0" />
	</xsl:param>
	
	<!-- Пропускать ли записи с пустой геометрией (0 - false, 1 - true) -->
	<xsl:param name="skip_empty_geom">
		<xsl:value-of select="0" />
	</xsl:param>
	
	<!-- Для выборки узлов используется язык XPath. Применяемые оси XPath descendant::node 
		- узлы-потомки элемента node ancestor::node - узлы-предки элемента node https://www.w3schools.com/xml/xpath_axes.asp -->

	<!-- Начало обработки входящего XML-документа -->
	<xsl:template match="/extract_cadastral_plan_territory">
		<xsl:element name="ShapeFiles">
			<xsl:element name="ShapeFile">
				<xsl:attribute name="prefix">
					<xsl:value-of select="'ZU'" />
				</xsl:attribute>
				<xsl:element name="FeatureType">
					<xsl:attribute name="geometry_type">
						<xsl:value-of select="'MultiPolygon'" />
					</xsl:attribute>

					<!-- Описание структуры атрибутивной таблицы -->
					<xsl:element name="Attributes">

						<xsl:call-template name="AttributeTemplate">
							<xsl:with-param name="name" select="'src_file'" />
							<xsl:with-param name="value" select="'String'" />
							<xsl:with-param name="use_value_as_type" select="true()" />
						</xsl:call-template>

						<xsl:call-template name="AttributeTemplate">
							<xsl:with-param name="name" select="'DateUpload'" />
							<xsl:with-param name="value" select="'String'" />
							<xsl:with-param name="use_value_as_type" select="true()" />
						</xsl:call-template>
								
						<xsl:call-template name="AttributeTemplate">
							<xsl:with-param name="name" select="'cad_num'" />
							<xsl:with-param name="value" select="'String'" />
							<xsl:with-param name="use_value_as_type" select="true()" />
						</xsl:call-template>

						<xsl:call-template name="AttributeTemplate">
							<xsl:with-param name="name" select="'cad_qrtr'" />
							<xsl:with-param name="value" select="'String'" />
							<xsl:with-param name="use_value_as_type" select="true()" />
						</xsl:call-template>

						<xsl:call-template name="AttributeTemplate">
							<xsl:with-param name="name" select="'area'" />
							<xsl:with-param name="value" select="'String'" />
							<xsl:with-param name="use_value_as_type" select="true()" />
						</xsl:call-template>
						
						<xsl:call-template name="AttributeTemplate">
							<xsl:with-param name="name" select="'sk_id'" />
							<xsl:with-param name="value" select="'String'" />
							<xsl:with-param name="use_value_as_type" select="true()" />
						</xsl:call-template>

						<xsl:call-template name="AttributeTemplate">
							<xsl:with-param name="name" select="'category'" />
							<xsl:with-param name="value" select="'String'" />
							<xsl:with-param name="use_value_as_type" select="true()" />
						</xsl:call-template>
						
						<xsl:call-template name="AttributeTemplate">
							<xsl:with-param name="name" select="'permit_use'" />
							<xsl:with-param name="value" select="'String'" />
							<xsl:with-param name="use_value_as_type" select="true()" />
						</xsl:call-template>

						<xsl:call-template name="AttributeTemplate">
							<xsl:with-param name="name" select="'address'" />
							<xsl:with-param name="value" select="'String'" />
							<xsl:with-param name="use_value_as_type" select="true()" />
						</xsl:call-template>
						
					</xsl:element>

				</xsl:element>
				<xsl:element name="Features">
					<xsl:apply-templates
						select="descendant::land_record" />
				</xsl:element>
			</xsl:element>
		</xsl:element>
	</xsl:template>

	<xsl:template match="land_record" mode="point_type">
		<!-- Здесь указываем mode="point_type" для вывода каждой координаты как самостоятельной Feature -->
			<xsl:apply-templates
				select="descendant::ordinate" mode="point_type" />
	</xsl:template>


	<xsl:template match="land_record">
		<xsl:variable name="count_geom" select="count(descendant::spatials_elements)" />

		<xsl:if test="$skip_empty_geom = 0 or $count_geom &gt; 0">
		
		<xsl:element name="Feature">
			<xsl:element name="Attributes">

				<xsl:call-template name="AttributeTemplate">
					<xsl:with-param name="name">
						<xsl:value-of select="'src_file'" />
					</xsl:with-param>
					<xsl:with-param name="value">
						<!-- Значение параметра -->
						<xsl:value-of select="$source_file" />
					</xsl:with-param>
				</xsl:call-template>
				
				<xsl:call-template name="AttributeTemplate">
					<xsl:with-param name="name">
						<xsl:value-of select="'DateUpload'" />
					</xsl:with-param>
					<xsl:with-param name="value">
						<xsl:value-of select="ancestor::extract_cadastral_plan_territory/details_request/date_received_request" />
					</xsl:with-param>
				</xsl:call-template>
	
				<xsl:call-template name="AttributeTemplate">
					<xsl:with-param name="name" select="'cad_num'"/>
					<xsl:with-param name="value">
						<!-- Значение находится внутри относительно текущего элемента -->
						<xsl:value-of select="object/common_data/cad_number" />
					</xsl:with-param>
				</xsl:call-template>

				<xsl:call-template name="AttributeTemplate">
					<xsl:with-param name="name">
						<xsl:value-of select="'cad_qrtr'" />
					</xsl:with-param>
					<xsl:with-param name="value">
							<xsl:value-of select="ancestor::cadastral_block/cadastral_number" />
					</xsl:with-param>
				</xsl:call-template>

				<xsl:call-template name="AttributeTemplate">
					<xsl:with-param name="name">
						<xsl:value-of select="'area'" />
					</xsl:with-param>
					<xsl:with-param name="value">
						<xsl:value-of select="params/area/value" />
					</xsl:with-param>
				</xsl:call-template>

				<xsl:call-template name="AttributeTemplate">
					<xsl:with-param name="name">
						<xsl:value-of select="'sk_id'" />
					</xsl:with-param>
					<xsl:with-param name="value">
						<xsl:value-of
							select="contours_location/contours/contour[1]/entity_spatial/sk_id" />
					</xsl:with-param>
				</xsl:call-template>
				
				<xsl:call-template name="AttributeTemplate">
					<xsl:with-param name="name">
						<xsl:value-of select="'category'" />
					</xsl:with-param>
					<xsl:with-param name="value">
						<xsl:value-of
							select="params/category/type/value" />
					</xsl:with-param>
				</xsl:call-template>
				
				<xsl:call-template name="AttributeTemplate">
					<xsl:with-param name="name">
						<xsl:value-of select="'permit_use'" />
					</xsl:with-param>
					<xsl:with-param name="value">
						<xsl:value-of
							select="params/permitted_use/permitted_use_established/by_document" />
					</xsl:with-param>
				</xsl:call-template>
				
				<xsl:call-template name="AttributeTemplate">
					<xsl:with-param name="name">
						<xsl:value-of select="'address'" />
					</xsl:with-param>
					<xsl:with-param name="value">
						<xsl:value-of
							select="address_location/address/readable_address" />
					</xsl:with-param>
				</xsl:call-template>
				
			</xsl:element>
			<xsl:element name="Geometry">
				<xsl:apply-templates
					select="descendant::spatials_elements" />
			</xsl:element>
		</xsl:element>
		</xsl:if>
	</xsl:template>

	<xsl:template match="spatials_elements">
		<xsl:element name="Shell">
			<xsl:apply-templates
				select="descendant::spatial_element[1]" />
			<xsl:choose>
				<xsl:when test="count(descendant::spatial_element) &gt; 1">
					<xsl:element name="Holes">
						<xsl:apply-templates
							select="descendant::spatial_element[position() &gt; 1]"
							mode="hole" />
					</xsl:element>
				</xsl:when>
			</xsl:choose>
		</xsl:element>
	</xsl:template>

	<xsl:template match="spatial_element">
		<xsl:apply-templates select="descendant::ordinate" />
	</xsl:template>

	<xsl:template match="spatial_element" mode="hole">
		<xsl:element name="Hole">
			<xsl:apply-templates
				select="descendant::ordinate" />
		</xsl:element>
	</xsl:template>

	<xsl:template match="ordinate">
		<xsl:element name="Coordinate">
			<xsl:attribute name="x">
				<xsl:choose>
					<xsl:when test="$swap_xy = 0">
						<xsl:value-of select="descendant::x" />
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="descendant::y" />
					</xsl:otherwise>
				</xsl:choose>
			</xsl:attribute>
			<xsl:attribute name="y">
				<xsl:choose>
					<xsl:when test="$swap_xy = 0">
						<xsl:value-of select="descendant::y" />
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="descendant::x" />
					</xsl:otherwise>
				</xsl:choose>
			</xsl:attribute>
		</xsl:element>
	</xsl:template>

	<!-- Вывод координат как самостоятельных точек -->
	<xsl:template match="ordinate" mode="point_type">
		<xsl:element name="Feature">
			<xsl:element name="Attributes">
				<xsl:call-template name="AttributeTemplate">
					<xsl:with-param name="name">
						<xsl:value-of select="'ord_nmb'" />
					</xsl:with-param>
					<xsl:with-param name="value">
						<xsl:value-of select="ord_nmb" />
					</xsl:with-param>
				</xsl:call-template>

				<xsl:call-template name="AttributeTemplate">
					<xsl:with-param name="name">
						<xsl:value-of select="'num_gpnt'" />
					</xsl:with-param>
					<xsl:with-param name="value">
						<xsl:value-of select="num_geopoint" />
					</xsl:with-param>
				</xsl:call-template>

				<xsl:call-template name="AttributeTemplate">
					<xsl:with-param name="name">
						<xsl:value-of select="'delta'" />
					</xsl:with-param>
					<xsl:with-param name="value">
						<xsl:value-of select="delta_geopoint" />
					</xsl:with-param>
				</xsl:call-template>
			</xsl:element>
			<xsl:element name="Geometry">
				<xsl:element name="Point">
					<!-- Вызов шаблона для текущего узла ordinate в режиме по умолчанию -->
					<xsl:apply-templates select="." />
				</xsl:element>
			</xsl:element>

		</xsl:element>
	</xsl:template>

	<xsl:template name="AttributeTemplate">
		<xsl:param name="name" />
		<xsl:param name="value" />
		<xsl:param name="use_value_as_type" select="false()" />

		<xsl:element name="Attribute">
			<xsl:attribute name="name">
				<xsl:value-of select="$name" />
			</xsl:attribute>
			<xsl:choose>
				<xsl:when test="$use_value_as_type = true()">
					<xsl:attribute name="type">
						<xsl:value-of select="$value" />
					</xsl:attribute>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="$value" />
				</xsl:otherwise>
			</xsl:choose>
		</xsl:element>
	</xsl:template>

</xsl:stylesheet>